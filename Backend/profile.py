import os
import uuid
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File
from sqlalchemy.orm import Session
from database import get_db
from models import Profile, Booking, User
from schemas import ProfileResponse, ProfileUpdateRequest
from auth import get_current_user

router = APIRouter(prefix="/profile")

AVATAR_DIR = "static/avatars"
os.makedirs(AVATAR_DIR, exist_ok=True)

ALLOWED_CONTENT_TYPES = {"image/jpeg": "jpg", "image/png" : "png", "image/webp": "webp"}
MAX_AVATAR_SIZE_BYTES = 5120 * 1024

TIERS = [
    ("Bronze", 0),
    ("Silver", 500),
    ("Gold", 1000),
    ("Platinum", 2500)
]

def compute_tier(points: int):
    curr_t = TIERS[0][0]
    curr_p = TIERS[0][1]
    next_t = None
    next_p = None

    for i, (name, threshold) in enumerate(TIERS):
        if points >= threshold:
            curr_t = name
            curr_p = threshold
            if i + 1 < len(TIERS):
                next_t, next_p = TIERS[i + 1]
            else:
                next_t = None
                next_p = None

    if next_p is None:
        points_next = None
        progress = 100.0
    else:
        points_next = next_p - points
        span = next_p - curr_p
        progress = ((points - curr_p) / span) * 100 if span > 0 else 100.0
        progress = max(0.0, min(100.0, progress))

    return curr_t, next_t, points_next, progress

def to_response(user: User, profile: Optional[Profile], total_bookings: int) -> ProfileResponse:
    points = profile.reward_points if profile else 0
    tier, next_tier, points_next, progress = compute_tier(points)
    return ProfileResponse(
    id = user.id,
    email = user.email,
    full_name = profile.full_name if profile else None,
    phone = profile.phone if profile else None,
    date_of_birth = profile.dob if profile else None,
    reward_points = points,
    fcm_token = profile.fcm_token if profile else None,
    avatar_url = profile.avatar_url if profile else None,
    total_bookings = total_bookings,
    loyalty_tier = tier,
    next_tier = next_tier,
    points_next_tier = points_next,
    tier_progress = progress
    )

@router.get("/me", response_model=ProfileResponse)
def get_my_profile(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    profile = db.query(Profile).filter(Profile.id == current_user.id).first()
    total_bookings = db.query(Booking).filter(Booking.user_id == current_user.id).count()
    return to_response(current_user, profile, total_bookings)

@router.put("/me", response_model=ProfileResponse)
def update_my_profile(data: ProfileUpdateRequest, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
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

@router.post("/me/avatar", response_model=ProfileResponse)
def upload_avatar(file : UploadFile = File(...), current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if file.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(status_code=400, detail="Only JPEG, PNG, and WEBP are allowed")
    content = file.file.read()
    if len(content) > MAX_AVATAR_SIZE_BYTES:
        raise HTTPException(status_code=400, detail="Image must be under 5MB")

    ext = ALLOWED_CONTENT_TYPES[file.content_type]
    filename = f"{current_user.id}_{uuid.uuid4().hex[:8]}.{ext}"
    filepath = os.path.join(AVATAR_DIR, filename)

    with open(filepath, "wb") as f:
        f.write(content)
    profile = db.query(Profile).filter(Profile.id == current_user.id).count()
    if not profile:
        profile = Profile(id=current_user.id)
        db.add(profile)
    profile.avatar_url = f"/{AVATAR_DIR}/{filename}"
    db.commit()
    db.refresh(profile)

    total_bookings = db.query(Booking).filter(Booking.user_id == current_user.id).count()
    return to_response(current_user, profile, total_bookings)