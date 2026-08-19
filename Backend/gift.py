from typing import List
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import func
from sqlalchemy.orm import Session
from database import get_db
from models import GiftCard, Salon, Service, User
from schemas import EmailExistResponse, GiftCardCreateRequest, GiftCardResponse
from auth import get_current_user

router = APIRouter()

@router.get("/user/exists", response_model=EmailExistResponse)
def check(email: str = Query(...), db: Session = Depends(get_db)):
    exists = db.query(User).filter(User.email == email).first() is not None
    return EmailExistResponse(exists=exists)

@router.post("/gifts", response_model=GiftCardResponse, status_code=201)
def send_gift(data: GiftCardCreateRequest, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    if data.receiver_email.strip().lower() == user.email.strip().lower():
        raise HTTPException(status_code=400, detail="You cannot send a gift to yourself")
    receiver = db.query(User).filter(func.lower(User.email) == data.receiver_email.strip().lower()).first()
    if not receiver:
        raise HTTPException(status_code=404, detail="No Lustre account found for that email")

    salon = db.query(Salon).filter(Salon.id == data.salon_id).first()
    if not salon:
        raise HTTPException(status_code=404, detail="Salon not found")

    if data.service_id:
        service = db.query(Service).filter(Service.id == data.service_id, Service.salon_id == data.salon_id).first()
        if not service:
            raise HTTPException(status_code=404, detail="Service not found for this salon")
        amount = service.price
    elif data.amount is not None:
        if data.amount <= 0:
            raise HTTPException(status_code=400, detail="Amount must be greater than zero")
        amount = data.amount
    else:
        raise HTTPException(status_code=400, detail="Provide either service_id or amount")

    gift = GiftCard(
        salon_id=data.salon_id,
        service_id=data.service_id,
        amount=amount,
        currency=salon.currency,
        occasion=data.occasion,
        message=data.message,
        sender_id=user.id,
        receiver_id=receiver.id
    )
    db.add(gift)
    db.commit()
    db.refresh(gift)
    return gift

@router.get("/gifts/received", response_model=List[GiftCardResponse])
def received_gifts(unused_only: bool = True, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    query = db.query(GiftCard).filter(GiftCard.receiver_id == user.id)
    if unused_only:
        query = query.filter(GiftCard.is_used == False)
    return query.order_by(GiftCard.created_at.desc()).all()

@router.get("/gifts/sent", response_model=List[GiftCardResponse])
def sent_gifts(db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    return db.query(GiftCard).filter(GiftCard.sender_id == user.id).order_by(GiftCard.created_at.desc()).all()