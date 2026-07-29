from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from database import get_db
from models import User, Profile
from schemas import UserResponse, SignIn, SignUp, TokenResponse, GoogleAuth
import os
from jose import jwt, JWTError
from dotenv import load_dotenv
from typing import Optional
from datetime import datetime, timedelta
from passlib.context import CryptContext
from fastapi.security import OAuth2PasswordBearer
from google.oauth2 import id_token as google_id_token
from google.auth.transport import requests as google_requests

load_dotenv()
SECRET_KEY = os.getenv("JWT_SECRET")
EXPIRE_MINUTES = 10080
GOOGLE_CLIENT_ID = os.getenv("GOOGLE_CLIENT_ID")
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")

router = APIRouter(prefix="/auth")



pwd_context = CryptContext(schemas=["bycrypt"], deprecated="auto")

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

@router.post("/signup", response_model=TokenResponse)
def signup(data: SignUp, db: Session = Depends(get_db)):
    existing = db.query(User).filter(User.email == data.email).first()
    if existing:
        raise HTTPException(status_code=400, detail="Email already registered")

    user = User(
    email = data.email,
    name = data.name,
    hashed_password = hash_password(data.password),
    auth_provide = "local"
    )
    db.add(user)
    db.flush()
    db.add(Profile(id=user.id, full_name=data.name))
    db.commit()

    return TokenResponse(access_token=create_access_token(user.id))

@router.post("/signin", response_model=TokenResponse)
def login(data: SignIn, db : Session = Depends(get_db)):
    user = db.query(User).filter(User.email == data.email).first()
    if not user or not user.hashed or not verify_password(data.password, user.hashed_password):
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
            db.add(Profile(id=user.id, full_name=name)
        db.commit()

    return TokenResponse(access_token=create_access_token(user.id))

@router.get("/me", response_model=UserResponse)
def get_me(current_user: User = Depends(get_current_user)):
    return current_user
