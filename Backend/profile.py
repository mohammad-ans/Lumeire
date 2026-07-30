from typing import Optional
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from database import get_db
from models import Profile, Booking, User
from schemas import ProfileResponse, ProfileUpdateRequest
from auth import get_current_user

router = APIRouter(prefix="/profile")

def to_response(user: User, profile: Optional[Profile], total_bookings: int) -> ProfileResponse:
    return ProfileResponse(
    id = user.id,
    email = user.email,
    full_name = profile.full_name if profile else None,
    phone = profile.phone if profile else None,
    date_of_birth = profile.dob if profile else None,
    reward_points = profile.reward_points,
    fcm_token = profile.fcm_token if profile else None,
    total_bookings = total_bookings
    )

@router.get("/me", response_model=ProfileResponse)
def get_my_profile(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    profile = db.query(Profile).filter(Profile.id == current_user.id).first()
    total_bookings = db.query(Booking).filter(Booking.user_id == current_user.id).count()
    return to_response(current_user, profile, total_bookings)

@router.put("/me", response_model=ProfileResponse)
def update_my_profile(data: ProfileUpdateRequest, current_user: user = Depends(get_current_user), db: Session = Depends(get_db)):
    profile = db.query(Profile).filter(Profile.id == current_user.id).first()
    if not profile:
        profile = Profile(id=current_user.id)
        db.add(profile)
    if data.full_name is not None:
        profile.full_name = data.full_name

    if data.phone is not None:
        profile.phone = data.phone

    if data.date_of_birth is not None:
        profile.dob = data.date_of_birth

    if data.fcm_token is not None:
        profile.fcm_token = data.fcm_token

    db.commit()
    db.refresh(profile)

    total_bookings = db.query(Booking).filter(Booking.user_id == current_user.id).count()
    return to_response(current_user, profile, total_bookings)