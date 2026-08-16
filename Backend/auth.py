from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from database import get_db
from models import User, Profile, PendingUser, PasswordReset
from schemas import UserResponse, SignIn, SignUp, TokenResponse, GoogleAuth, MessageResponse, Otp, ResendOtp, ForgotPassword, ResetPassword
import os
from jose import JWTError, jwt
from dotenv import load_dotenv
from typing import Optional
from datetime import datetime, timedelta
from passlib.context import CryptContext
from fastapi.security import OAuth2PasswordBearer
from google.oauth2 import id_token as google_id_token
from google.auth.transport import requests as google_requests
import secrets
from email_send import send_otp
from vouchers import onboard_new_user


load_dotenv()
SECRET_KEY = os.getenv("JWT_SECRET")
EXPIRE_MINUTES = 10080
GOOGLE_CLIENT_ID = os.getenv("GOOGLE_CLIENT_ID")
OTP_EXPIRE_MINUTES = 2
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")

router = APIRouter(prefix="/auth")


pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def hash_password(password: str) -> str:
    return pwd_context.hash(password)

def verify_password(plain: str, hashed: str):
    return pwd_context.verify(plain, hashed)

def create_access_token(user : str, expires_delta: Optional[timedelta] = None) -> str:
    expire = datetime.utcnow() + (expires_delta or timedelta(minutes=EXPIRE_MINUTES))
    to_encode = {"sub" : user, "exp" : expire}
    return jwt.encode(to_encode, SECRET_KEY, algorithm="HS256")

def decode_access_token(token: str) -> Optional[str]:
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=["HS256"])
        return payload.get("sub")
    except:
        return None

def get_current_user(token: str = Depends(oauth2_scheme), db: Session = Depends(get_db)) -> User:
    user_id = decode_access_token(token)
    if user_id is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid pr expired token")
    user = db.query(User).filter(User.id == user_id).first()
    if user is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")
    return user

def generate_otp(length: int = 6) -> str:
    return "".join(secrets.choice("0123456789") for _ in range(length))

@router.post("/signup", response_model=MessageResponse)
async def signup(data: SignUp, db: Session = Depends(get_db)):
    existing = db.query(User).filter(User.email == data.email).first()
    if existing:
        raise HTTPException(status_code=400, detail="Email already registered")
    otp_code = generate_otp()
    expires_at = datetime.utcnow() + timedelta(minutes=OTP_EXPIRE_MINUTES)
    try:
        response = await send_otp(data.email, otp_code)
    except Exception as e:
        print(e)
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Service not available")
    pending = db.query(PendingUser).filter(PendingUser.email == data.email).first()
    if pending:
        pending.name = data.full_name
        pending.hashed_password = hash_password(data.password)
        pending.otp_code = otp_code
        pending.otp_expires_at = expires_at
        pending.created_at = datetime.utcnow()
        pending.referred_by_code = data.referral_code
    else:
        pending = PendingUser(
        email = data.email,
        name = data.full_name,
        hashed_password = hash_password(data.password),
        otp_code = otp_code,
        otp_expires_at = expires_at,
        referred_by_code = data.referral_code
        )
        db.add(pending)
    db.commit()
    return MessageResponse(message="Verification code sent to your email")

@router.post("/verify", response_model=TokenResponse)
def verify_otp(data: Otp, db : Session = Depends(get_db)):
    pending = db.query(PendingUser).filter(PendingUser.email == data.email).first()
    if not pending or pending.otp_code != data.otp_code:
        raise HTTPException(status_code=400, detail="Invalid verification code")

    if pending.otp_expires_at is None or pending.otp_expires_at < datetime.utcnow():
        raise HTTPException(status_code=400, detail="Verification code expired, please request a new one")

    if db.query(User).filter(User.email == pending.email).first():
        db.delete(pending)
        db.commit()
        raise HTTPException(status_code=400, detail="Email already registered")

    user = User(
    email=pending.email,
    name=pending.name,
    hashed_password=pending.hashed_password,
    auth_provider="local",
    verified=True
    )
    db.add(user)
    db.flush()
    db.add(Profile(id=user.id, full_name=pending.name))
    onboard_new_user(db, user, pending.referred_by_code)
    db.delete(pending)
    db.commit()
    db.refresh(user)

    return TokenResponse(access_token=create_access_token(user.id))

@router.post("/resend-otp", response_model=MessageResponse)
async def resend_otp(data: ResendOtp, db: Session = Depends(get_db)):
    pending = db.query(PendingUser).filter(PendingUser.email == data.email).first()
    if not pending:
        raise HTTPException(status_code=404, detail="No pending signup found for this email")
    otp_code = generate_otp()
    try:
        response = await send_otp(data.email, otp_code)
    except:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Service not available")
    pending.otp_code = otp_code
    pending.otp_expires_at = datetime.utcnow() + timedelta(minutes=OTP_EXPIRE_MINUTES)
    db.commit()

    return MessageResponse(message="Verification code resent")

@router.post("/signin", response_model=TokenResponse)
def login(data: SignIn, db : Session = Depends(get_db)):
    user = db.query(User).filter(User.email == data.email).first()
    if not user or not user.hashed_password or not verify_password(data.password, user.hashed_password):
        raise HTTPException(status_code=401, detail="Incorrect email or password")

    return TokenResponse(access_token=create_access_token(user.id))

@router.post("/google", response_model=TokenResponse)
def g_auth(data: GoogleAuth, db : Session = Depends(get_db)):
    if not GOOGLE_CLIENT_ID:
        raise HTTPException(status_code=500, detail="Google client id not configured")

    try:
        id_info = google_id_token.verify_oauth2_token(data.id_token, google_requests.Request(), GOOGLE_CLIENT_ID)
    except:
        raise HTTPException(status_code=401, detail="Invalid Google ID token")

    google_sub = id_info["sub"]
    email = id_info.get("email")
    name = id_info.get("name")

    user = db.query(User).filter(User.google_id == google_sub).first()

    if not user:
        user = db.query(User).filter(User.email == email).first()
        if user:
            user.google_id = google_sub
            if not user.name:
                user.name = name
        else:
            user = User(
                email=email,
                name=name,
                google_id=google_sub,
                auth_provider="google",
                verified=True
            )
            db.add(user)
            db.flush()
            db.add(Profile(id=user.id, full_name=name))
            onboard_new_user(db, user, data.referral_code)
        db.commit()

    return TokenResponse(access_token=create_access_token(user.id))

@router.post("/forgot-password", response_model=MessageResponse)
async def forgot_password(data: ForgotPassword, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == data.email).first()
    if not user:
        raise HTTPException(status_code=404, detail="No account found for this email")

    otp_code = generate_otp()
    expires_at = datetime.utcnow() + timedelta(minutes=OTP_EXPIRE_MINUTES)

    reset = db.query(PasswordReset).filter(PasswordReset.email == data.email).first()
    if reset:
        reset.otp_code = otp_code
        reset.expires_at = expires_at
        reset.created_at = datetime.utcnow()
    else:
        reset = PasswordReset(
            email=data.email,
            otp_code=otp_code,
            expires_at=expires_at
        )
        db.add(reset)
    try:
        await send_otp(data.email, otp_code)
    except:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail="Service not available")
    db.commit()

    return MessageResponse(message="Verification code sent to your email")

@router.post("/reset-password", response_model=MessageResponse)
def reset_password(data: ResetPassword, db: Session = Depends(get_db)):
    reset = db.query(PasswordReset).filter(PasswordReset.email == data.email).first()
    if not reset or reset.otp_code != data.otp_code:
        raise HTTPException(status_code=400, detail="Invalid verification code")

    if reset.expires_at < datetime.utcnow():
        db.delete(reset)
        db.commit()
        raise HTTPException(status_code = 400, detail="Verification code expired, request a new one")

    user = db.query(User).filter(User.email == data.email).first()
    if not user:
        db.delete(reset)
        db.commit()
        raise HTTPException(status_code=404, detail="User not found")
    user.hashed_password = hash_password(data.new_password)
    db.delete(reset)
    db.commit()

    return MessageResponse(message="Password reset, login by using new password")

@router.get("/me", response_model=UserResponse)
def get_me(current_user: User = Depends(get_current_user)):
    return current_user
