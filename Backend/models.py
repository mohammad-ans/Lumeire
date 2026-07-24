from sqlalchemy import Column, Integer, DateTime, String, Float, ForeignKey
from sqlalchemy.orm import declarative_base

Base = declarative_base()
class User(Base):
    __tablename__ = "users"
    email = Column(String, primary_key=True, index=True)
    name = Column(String)
    password = Column(String)

class Pending_user(Base):
    __tablename__ = "pending_users"

    email = Column(String, primary_key=True, index=True)
    name = Column(String)
    password = Column(String)

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
