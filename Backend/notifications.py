from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List, Optional
from database import get_db
from models import Notification, User, Profile
from schemas import NotificationResponse, MessageResponse
from auth import get_current_user
from push import send_push

router = APIRouter(prefix="/notifications")

def create_notification(db: Session, id: str, t: str, b: str, type: str = "general", r: Optional[str] = None, commit: bool = True) -> Notification:
    notification = Notification(user_id=id, title=t, body=b, type=type, related_booking_id=r)
    db.add(notification)
    db.flush()
    if commit:
        db.commit()
        db.refresh(notification)

    profile = db.query(Profile).filter(Profile.id == id).first()
    token = profile.fcm_token if profile else None
    if token:
        send_push(token, t, b, data={"type": type, "id": notification.id, "related_booking_id": r})
    return notification

@router.get("", response_model=List[NotificationResponse])
def list_notifications(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return db.query(Notification).filter(Notification.user_id == user.id).order_by(Notification.created_at.desc()).all()

@router.get("/unreadCount")
def unread_count(db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    count = db.query(Notification).filter(Notification.user_id == user.id, Notification.is_read == False).count()
    return count

@router.patch("/{id}/read", response_model=NotificationResponse)
def mark_read(id: str, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    notification = db.query(Notification).filter(Notification.id == id, Notification.user_id == user.id).first()
    if not notification:
        raise HTTPException(status_code=404, detail="Notification not found")
    notification.is_read = True
    db.commit()
    db.refresh(notification)
    return notification

@router.patch("/readAll")
def mark_all(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    db.query(Notification).filter(Notification.user_id == user.id, Notification.is_read == False).update({Notification.is_read: True})
    db.commit()
    return MessageResponse(message="All notifications marked as read")