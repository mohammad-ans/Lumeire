from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from database import get_db
from models import User
from schemas import UserResponse, SignIn, SignUp, TokenResponse
import os
from jose import jwt, JWTError
from dotenv import load_dotenv
from typing import Optional
from datetime import datetime, timedelta
from passlib.context import CryptContext

load_dotenv()
SECRET_KEY = os.getenv("JWT_SECRET")
EXPIRE_MINUTES = 10080

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

