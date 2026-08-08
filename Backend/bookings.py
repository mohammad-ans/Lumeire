from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import List
from database import get_db
from models import Booking, Salon, Service, Stylist, User, GiftCard
from schemas import BookingCreateRequest, BookingResponse, SalonResponse, ServiceResponse,

from auth import get_current_user

router = APIRouter()

@router.get("/salons", response_model=List[SalonResponse])
def list_salons(category: Optional[str] = Query(None), search: Optional[str] = Query(None), db: Session = Depends(get_db)):
    query = db.query(Salon)
    if category:
        query = query.filter(Salon.category.ilike(f"%{category}%"))
    if search:
        query = query.filter(Salon.name.ilike(f"%{search}%"))
    return query.all()

@router.get("/salons/{salon_id}", response_model=SalonResponse)
def gsalon(salon_id: str, db: Session = Depends(get_db)):
    salon = db.query(Salon).filter(Salon.id == salon_id).first()
    if not salon:
        raise HTTPException(status_code=404, detail="Salon Not found")
    return salon

@router.get("/salons/{salon_id}/services", response_model=List[ServiceResponse])
def list_services(salon_id: str, db: Session = Depends(get_db)):
    salon = db.query(Salon).filter(Salon.id == salon_id).first()
    if not salon:
        raise HTTPException(status_code=404, detail="Salon Not Found")
    return db.query(Service).filter(Service.salon_id == salon_id).all()


@router.post("/bookings", response_model=BookingResponse, status_code=201)
def create_booking(data: BookingCreateRequest, db: Session = Depends(get_db), current_user: Depends(get_current_user)):
    service = db.query(Service).filter(Service.id == data.service_id, Service.salon_id == data.salon_id).first()
    if not service:
        raise HTTPException(status_code=404, detail="Service not found for this salon")
    if data.stylist_id:
        stylist = db.query(Stylist).filter(Stylist.id == data.stylist_id, Stylist.salon_id == data.salon_id).first()
        if not stylist:
            raise HTTPException(status_code=404, detail="Stylist not found for this salon")

    amount_due = service.price
    gift_card = None
    if data.gift_card_id:
        gift_card = db.query(GiftCard.id == payload.gift_card_id, GiftCard.receiver_id == current_user.id).with_for_update().first()
        if not gift_card:
            raise HTTPException(status_code=404, detail="Gift card not found")
        if gift_card.is_used:
            raise HTTPException(status_code=400, detail="Gift card has already been used")
        if gift_card.salon_id != data.salon_id:
            raise HTTPException(status_code=400, detail="The gift card can only be used at the salon it was issued at")

        amount_due = max(service.price - gift_card.amount, 0.0)
    booking = Booking(
    user_id = current_user.id,
    salon_id = data.salon_id,
    stylist_id = data.stylist_id,
    appointment_time = data.appointment_time,
    status = "Upcoming",
    total_amount = amount_due,
    currency=service.salon.currency,
    payment_status = "paid" if amount_due <= 0 else "unpaid"
    )
    db.add(booking)
    db.flush()
    if gift_card:
        gift_card.is_used = True
        gift.redeemed_booking_id = booking.id

    db.commit()
    db.refresh(booking)
    return booking

@router.get("/bookings", response_model=List[BookingResponse])
def list_bookings(db : Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    return db.query(Booking).filter(Booking.user_id == current_user.id).order_by(Booking.appointment_time.desc()).all()

@router.patch("/bookings/{booking_id}/mark-paid", response_model=BookingResponse)
def mark_paid(booking_id: str, db: Session = Depends(get_db), user: User = Depends(get_db)):
    booking = db.query(Booking).filter(Booking.id == booking_id, Booking.user_id == user.id)
    if not booking:
        raise HTTPException(status_code=404, detail="Booking not found")
    if booking.status == "Cancelled":
        raise HTTPException(status_code=400, detail="Booking is cancelled")
    if booking.payment_status != "unpaid":
        raise HTTPException(status_code=400, detail=f"Booking is already '{booking.payment_status}'")

    booking.payment_status = "paid"
    db.commit()
    db.refresh(booking)
    return booking

@router.delete("/bookings/{booking_id}", response_model=BookingResponse)
def cancel_booking(booking_id: str, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    booking = db.query(Booking).filter(Booking.id == booking_id, Booking.user_id == current_user.id).first()
    if not booking:
        raise HTTPException(status_code=404, detail="Booking Not found")
    if booking.status == "Cancelled":
        raise HTTPException(status_code=400, detail="Booking is already cancelled")
    redeemed_gift = db.query(GiftCard).filter(GiftCard.redeemed_booking_id == booking.id).first()
    if redeemed_gift:
        redeemed_gift.is_used = False
        redeemed_gift.redeemed_booking_id = None

    if booking.payment_status == "paid":
        booking.payment_status = "refund_due" if booking.total_amount > 0 else "refunded"
    booking.status = "Cancelled"
    db.commit()
    db.refresh(booking)
    return booking