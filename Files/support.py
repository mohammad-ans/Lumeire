from typing import List
from fastapi import APIRouter, Depends, HTTPException
from database import get_db
from sqlalchemy.orm import Session
from models import SupportTicket, User
from schemas import SupportTicketCreate, SupportTicketResponse
from auth import get_current_user

router = APIRouter(prefix="/support")

@router.post("/tickets", response_model=SupportTicketResponse)
def create(data: SupportTicketCreate, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    subject = data.sbj.strip()
    message = data.msg.strip()
    if not subject or not message:
        raise HTTPException(status_code = 400, detail="Subject and message are required")

    ticket = SupportTicket(
        user_id = user.id,
        subject=subject,
        message=message
    )
    db.add(ticket)
    db.commit()
    db.refresh(ticket)
    return ticket

@router.get("/tickets", response_model=List[SupportTicketResponse])
def list_tickets(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return db.query(SupportTicket).filter(SupportTicket.user_id == user.id).order_by(SupportTicket.created_at.desc()).all()