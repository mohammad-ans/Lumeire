from sqlalchemy import Column, Integer, DateTime, String, Float, ForeignKey
import uuid
from datetime import datetime
from sqlalchemy.dialects.postgresql import UUID
from database import Base
from sqlalchemy.orm import relationship


def gen_uuid() -> str:
    return str(uuid.uuid4())

class User(Base):
    __tablename__ = "users"
    id = Column(UUID(as_uuid=False), primary_key=True, default=gen_uuid)
    email = Column(String, unique=True, index=True, nullable=False)
    name = Column(String)

    hashed_password = Column(String)
    auth_provider = Column(String, default="local")
    google_id = Column(String, unique=True)
    verified = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)

class Pending_user(Base):
    __tablename__ = "pending_users"

    email = Column(String, primary_key=True, index=True)
    name = Column(String)
    hashed_password = Column(String)
    otp_code = Column(String)
    created_at = Column(DateTime, default=datetime.utcnow)

class Salon(Base):
    __tablename__ = "salons"
    id = Column(Integer, autoincrement=True, primary_key=True)
    name = Column(String)
    category = Column(String)
    rating = Column(Float)
    address = Column(String)
    longitude = Column(String)
    latitude = Column(String)
    openTime = Column(String)
    closeTime = Column(String)

class Booking(Base):
    __tablename__ = "bookings"
    booking_id = Column(Integer, autoincrement=True, primary_key=True)
    salon_id = Column(Integer, ForeignKey("salons.id"))
    user = Column(String, ForeignKey("users.email"))
    schedule = Column(DateTime)

class GiftCard(Base):
    __tablename__ = "gifts"
    gift_id = Column(Integer, autoincrement=True, primary_key=True)
    amount = Column(Integer)
    currency = Column(String)
    sender = Column(Integer, ForeignKey("users.email"))
    receiver = Column(Integer, ForeignKey("users.email"))
