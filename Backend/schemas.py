from typing import Optional
from pydantic import BaseModel, EmailStr, ConfigDict
from datetime import datetime

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

class SalonResponse(BaseModel):
    id: str
    name: str
    category: str
    address: str
    latitude: str
    longitude: str
    rating: float = 0.0
    review_count: int = 0
    phone: Optional[str] = None
    website: Optional[str] = None
    openTime: Optional[str] = None
    closeTime: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)

class ServiceResponse(BaseModel):
    id: str
    name: str
    category: Optional[str] = None
    duration_minutes: int
    price: float
    salon_id: str

class BookingCreateRequest(BaseModel):
    salon_id: str
    service_id: str
    stylist_id: Optional[str] = None
    appointment_time: datetime

class BookingResponse(BaseModel):
    id: str
    user-id: str
    salon_id: str
    stylist_id: Optional[str] = None
    appointment_time: datetime
    status: str
    total_amount: float
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)