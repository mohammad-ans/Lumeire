from typing import Optional
from pydantic import BaseModel, EmailStr, ConfigDict
from datetime import datetime

class SignUp(BaseModel):
    email: EmailStr
    password: str
    full_name: str
    referral_code: Optional[str] = None

class SignIn(BaseModel):
    email: EmailStr
    password: str

class ResetPassword(BaseModel):
    email: str
    otp_code: str
    new_password: str

class ForgotPassword(BaseModel):
    email: str

class GoogleAuth(BaseModel):
    id_token: str
    referral_code: Optional[str] = None

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

class UserResponse(BaseModel):
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
    avatar_url: Optional[str] = None
    total_bookings: int = 0
    loyalty_tier: str = "Bronze"
    next_tier: Optional[str] = None
    points_next_tier: Optional[int] = None
    tier_progress: float = 0.0

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
    image_url: Optional[str] = None
    currency: str = "USD"

    model_config = ConfigDict(from_attributes=True)

class ServiceResponse(BaseModel):
    id: str
    name: str
    category: Optional[str] = None
    duration_minutes: int
    price: float
    salon_id: str

    model_config = ConfigDict(from_attributes=True)

class BookingCreateRequest(BaseModel):
    salon_id: str
    service_id: str
    stylist_id: Optional[str] = None
    appointment_time: datetime
    gift_card_id: Optional[str] = None
    voucher_id: Optional[str] = None

class BookingResponse(BaseModel):
    id: str
    user_id: str
    salon_id: str
    stylist_id: Optional[str] = None
    appointment_time: datetime
    status: str
    total_amount: float
    currency: str = "USD"
    payment_status: str
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

class EmailExistResponse(BaseModel):
    exists: bool

class GiftCardCreateRequest(BaseModel):
    receiver_email: EmailStr
    salon_id: str
    service_id: Optional[str] = None
    amount: Optional[float] = None
    occasion: Optional[str] = None
    message: Optional[str] = None

class GiftCardResponse(BaseModel):
    id: str
    salon_id: Optional[str] = None
    salon_name: str
    service_id: Optional[str] = None
    amount: float
    currency: str = "USD"
    occasion: Optional[str] = None
    message: Optional[str] = None
    sender_id: str
    receiver_id: str
    is_used: bool
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

class VoucherResponse(BaseModel):
    id: str
    code: str
    discount_type: str
    discount_value: float
    reason: Optional[str] = None
    is_used: bool
    expires_at: Optional[datetime] = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

class ReferralInfoResponse(BaseModel):
    referral_code: str
    share_message: str

class PasswordChangeReq(BaseModel):
    password: str
    new_password: str

class SupportTicketCreate(BaseModel):
    sbj: str
    msg: str

class SupportTicketResponse(BaseModel):
    id: str
    subject: str
    message: str
    status: str
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)

class NotificationResponse(BaseModel):
    id: str
    title: str
    body: str
    type: str
    is_read: bool
    created_at: datetime
    related_booking_id: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)