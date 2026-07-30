from typing import Optional
from pydantic import BaseModel, EmailStr, ConfigDict

class SignUp(BaseModel):
    email: EmailStr
    password: str
    name: str

class SignIn(BaseModel):
    email: EmailStr
    password: str

class GoogleAuth(BaseModel):
    id_token: str

class Otp(BaseModel):
    email: EmailStr
    otp_code: str

class MessageResponse(BaseModel):
    message: str

class ResendOtp(BaseModel):
    email: EmailStr

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"

class UserResponse(BaseMode):
    id: str
    email: str
    name: Optional[str]  = None
    auth_provider: str
    is_verified: str

    model_config = ConfigDict(from_attributes=True)

class ProfileResponse(BaseModel):
    id: str
    email: str
    full_name: Optional[str] = None
    phone: Optional[str] = None
    date_of_birth: Optional[str] = None
    reward_points: int = 0
    fcm_token: Optional[str] = None
    total_bookings: int = 0

    model_config = ConfigDict(from_attributes=True)

class ProfileUpdateRequest(BaseModel):
    full_name: Optional[str] = None
    phone: Optional[str] = None
    date_of_birth: Optional[str] = None
    fcm_token: Optional[str] = None