from sqlalchemy import Column, Integer, DateTime, String, Float, Boolean, ForeignKey
import uuid
from datetime import datetime
from sqlalchemy.dialects.postgresql import UUID
from database import Base
from sqlalchemy.orm import relationship
import secrets

def gen_uuid() -> str:
    return str(uuid.uuid4())

def generate_referral_code() -> str:
    return secrets.token_hex(4).upper()

class User(Base):
    __tablename__ = "users"
    id = Column(UUID(as_uuid=False), primary_key=True, default=gen_uuid)
    email = Column(String, unique=True, index=True, nullable=False)
    name = Column(String)

    hashed_password = Column(String)
    auth_provider = Column(String, default="local")
    google_id = Column(String, unique=True)
    verified = Column(Boolean, default=False)
    referral_code = Column(String, unique = True, default=generate_referral_code)
    created_at = Column(DateTime, default=datetime.utcnow)
    profile = relationship("Profile", back_populates="user", uselist=False, cascade="all, delete-orphan")
    bookings = relationship("Booking", back_populates="user", cascade="all, delete-orphan")

class PendingUser(Base):
    __tablename__ = "pending_users"

    email = Column(String, primary_key=True, index=True)
    name = Column(String)
    hashed_password = Column(String)
    otp_code = Column(String)
    otp_expires_at = Column(DateTime)
    referred_by_code = Column(String, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)

class PasswordReset(Base):
    __tablename__ = "password_resets"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, index=True, nullable=False)
    otp_code = Column(String, nullable=False)
    expires_at = Column(DateTime, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)

class Profile(Base):
    __tablename__ = "profiles"
    id = Column(UUID(as_uuid=False), ForeignKey("users.id"), primary_key=True)
    full_name = Column(String)
    phone = Column(String)
    dob = Column(String)
    reward_points = Column(Integer, default=0)
    fcm_token = Column(String)
    avatar_url = Column(String)
    user = relationship("User", back_populates="profile")

class Salon(Base):
    __tablename__ = "salons"
    id = Column(UUID(as_uuid=False), primary_key=True, default=gen_uuid)
    name = Column(String)
    category = Column(String)
    address = Column(String)
    latitude = Column(Float)
    longitude = Column(Float)
    rating = Column(Float, default=0.0)
    review_count = Column(Integer, default=0)
    phone = Column(String)
    website = Column(String)
    openTime = Column(String)
    closeTime = Column(String)
    image_url = Column(String)
    currency = Column(String, default="USD")

    services = relationship("Service", back_populates="salon", cascade="all, delete-orphan")
    stylists = relationship("Stylist", back_populates="salon", cascade="all, delete-orphan")
    bookings = relationship("Booking", back_populates="salon")

class Service(Base):
    __tablename__ = "services"
    id = Column(UUID(as_uuid=False), primary_key=True, default=gen_uuid)
    name = Column(String)
    category = Column(String)
    duration_minutes = Column(Integer)
    price = Column(Float)
    salon_id = Column(UUID(as_uuid=False), ForeignKey("salons.id"), nullable=False)
    salon = relationship("Salon", back_populates="services")

    @property
    def currency(self) -> str:
        return self.salon.currency if self.salon else "USD"

class Stylist(Base):
    __tablename__ = "stylists"
    id = Column(UUID(as_uuid=False), primary_key=True, default=gen_uuid)
    name = Column(String, nullable=False)
    speciality = Column(String)
    salon_id = Column(UUID(as_uuid=False), ForeignKey("salons.id"), nullable=False)
    salon = relationship("Salon", back_populates="stylists")
    bookings = relationship("Booking", back_populates="stylist")

class Booking(Base):
    __tablename__ = "bookings"

    id = Column(UUID(as_uuid=False), primary_key=True, default=gen_uuid)
    user_id = Column(UUID(as_uuid=False), ForeignKey("users.id"), nullable=False)
    salon_id = Column(UUID(as_uuid=False), ForeignKey("salons.id"), nullable=False)
    stylist_id = Column(UUID(as_uuid=False), ForeignKey("stylists.id"), nullable=True)
    appointment_time = Column(DateTime, nullable=False)
    status = Column(String, default="Upcoming")
    total_amount = Column(Float, nullable=False)
    currency = Column(String, default="USD")
    created_at = Column(DateTime, default=datetime.utcnow)
    payment_status = Column(String, default="unpaid")

    user = relationship("User", back_populates="bookings")
    salon = relationship("Salon", back_populates="bookings")
    stylist = relationship("Stylist", back_populates="bookings")

class GiftCard(Base):
    __tablename__ = "gifts"

    id = Column(UUID(as_uuid=False), primary_key=True, default=gen_uuid)
    salon_id = Column(UUID(as_uuid=False), ForeignKey("salons.id"), nullable=True)
    service_id = Column(UUID(as_uuid=False), ForeignKey("services.id"), nullable=True)
    amount = Column(Float, nullable=False)
    currency = Column(String, default="USD")
    occasion = Column(String)
    message = Column(String)
    sender_id = Column(UUID(as_uuid=False), ForeignKey("users.id"), nullable=False)
    receiver_id = Column(UUID(as_uuid=False), ForeignKey("users.id"), nullable=False)
    is_used = Column(Boolean, default=False)

    redeemed_booking_id = Column(UUID(as_uuid=False), ForeignKey("bookings.id"), nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    salon = relationship("Salon")
    service = relationship("Service")
    sender = relationship("User", foreign_keys=[sender_id])
    receiver = relationship("User", foreign_keys=[receiver_id])

    @property
    def salon_name(self) -> str:
        return self.salon.name if self.salon else ""

    @property
    def sender_name(self) -> str:
        return self.sender.name if self.sender and self.sender.name else (self.sender.email if self.sender else "")
    @property
    def receiver_name(self) -> str:
        return self.receiver_name.name if self.receiver and self.receiver.name else (self.receiver.email if self.receiver else "")

class Voucher(Base):
    __tablename__ = "vouchers"
    id = Column(UUID(as_uuid=False), primary_key=True, default=gen_uuid)
    user_id = Column(UUID(as_uuid=False), ForeignKey("users.id"), nullable=False)
    code = Column(String, unique=True, nullable=False)
    discount_type = Column(String, default="percent")
    discount_value = Column(Float, nullable=False)
    reason = Column(String)
    is_used = Column(Boolean, default=False)
    redeemed_booking_id = Column(UUID(as_uuid=False), ForeignKey("bookings.id"), nullable=True)
    expires_at = Column(DateTime)
    created_at = Column(DateTime, default=datetime.utcnow)

    user = relationship("User")

class SupportTicket(Base):
    __tablename__ = "support_tickets"

    id = Column(UUID(as_uuid=False), primary_key=True, default=gen_uuid)
    user_id = Column(UUID(as_uuid=False), ForeignKey("users.id"), nullable=False)
    subject = Column(String, nullable=False)
    message = Column(String, nullable=False)
    status = Column(String, default="open")
    created_at = Column(DateTime, default=datetime.utcnow)

    user = relationship("User")

class Notification(Base):
    __tablename__ = "notifications"
    id = Column(UUID(as_uuid=False), primary_key=True, default=gen_uuid)
    user_id = Column(UUID(as_uuid=False), ForeignKey("users.id"), nullable=False)
    title = Column(String, nullable=False)
    body = Column(String, nullable=False)
    type = Column(String, default="general")
    related_booking_id = Column(UUID(as_uuid=False), ForeignKey("bookings.id"), nullable=True)
    is_read = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)

    user = relationship("User")