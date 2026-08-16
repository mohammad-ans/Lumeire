from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import List, Optional
from database import get_db
from models import Booking, Salon, Service, Stylist, User, GiftCard, Voucher
from schemas import BookingCreateRequest, BookingResponse, SalonResponse, ServiceResponse, StylistResponse

from datetime import datetime
from auth import get_current_user
from notifications import create_notification

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
def create_booking(data: BookingCreateRequest, db: Session = Depends(get_db), current_user= Depends(get_current_user)):
    service = db.query(Service).filter(Service.id == data.service_id, Service.salon_id == data.salon_id).first()
    if not service:
        raise HTTPException(status_code=404, detail="Service not found for this salon")
    if data.stylist_id:
        stylist = db.query(Stylist).filter(Stylist.id == data.stylist_id, Stylist.salon_id == data.salon_id).first()
        if not stylist:
            raise HTTPException(status_code=404, detail="Stylist not found for this salon")

    amount_due = service.price
    gift_card = None
    voucher = None
    if data.gift_card_id:
        gift_card = db.query(GiftCard).filter(GiftCard.id == data.gift_card_id, GiftCard.receiver_id == current_user.id).with_for_update().first()
        if not gift_card:
            raise HTTPException(status_code=404, detail="Gift card not found")
        if gift_card.is_used:
            raise HTTPException(status_code=400, detail="Gift card has already been used")
        if gift_card.salon_id is not None and gift_card.salon_id != data.salon_id:
            raise HTTPException(status_code=400, detail="The gift card can only be used at the salon it was issued at")

        amount_due = max(service.price - gift_card.amount, 0.0)

    if data.voucher_id:
        voucher = db.query(Voucher).filter(Voucher.id == data.voucher_id, Voucher.user_id == current_user.id).with_for_update().first()
        if voucher.is_used:
            raise HTTPException(status_code=400, detail="Voucher has already been used")
        if voucher.expires_at and voucher.expires_at < datetime.utcnow():
            raise HTTPException(status_code=400, detail="Vouvher has expired")
        if voucher.discount_type == "percent":
            amount_due = max(amount_due - (amount_due * voucher.discount_value / 100.0), 0.0)
        else:
            amount_due = max(amount_due - voucher.discount_value, 0.0)

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
        gift_card.redeemed_booking_id = booking.id
    if voucher:
        voucher.is_used = True
        voucher.redeemed_booking_id = booking.id
    db.commit()
    create_notification(db, current_user.id, "Booking confirmed", f"Your appointment on {booking.appointment_time.strftime('%b %d, %Y at %I:%M %p')} has been booked,", type="booking", r = booking.id)
    db.refresh(booking)
    return booking

@router.get("/bookings", response_model=List[BookingResponse])
def list_bookings(db : Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    finalize_bookings(db, current_user.id)
    return db.query(Booking).filter(Booking.user_id == current_user.id).order_by(Booking.appointment_time.desc()).all()

@router.patch("/bookings/{booking_id}/mark-paid", response_model=BookingResponse)
def mark_paid(booking_id: str, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    booking = db.query(Booking).filter(Booking.id == booking_id, Booking.user_id == user.id).first()
    if not booking:
        raise HTTPException(status_code=404, detail="Booking not found")
    if booking.status == "Cancelled":
        raise HTTPException(status_code=400, detail="Booking is cancelled")
    if booking.payment_status != "unpaid":
        raise HTTPException(status_code=400, detail=f"Booking is already '{booking.payment_status}'")

    booking.payment_status = "paid"
    db.commit()
    create_notification(db, user.id, "Payment received", f"We have received your payment for your upcoming appointment.", type="booking", r = booking.id)

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
    redeemed_voucher = db.query(Voucher).filter(Voucher.redeemed_booking_id == booking.id).first()
    if redeemed_voucher:
        redeemed_voucher.is_used = False
        redeemed_voucher.redeemed_booking_id = None

    if booking.payment_status == "paid":
        booking.payment_status = "refund_due" if booking.total_amount > 0 else "refunded"
    booking.status = "Cancelled"
    db.commit()
    create_notification(db, current_user.id, "Booking cancelled", f"Your appointment on {booking.appointment_time.strftime('%b %d, %Y at %I:%M %p')} has been cancelled,", type="booking", r = booking.id)

    db.refresh(booking)
    return booking

@router.get("/salons/{id}/stylists", response_model=List[StylistResponse])
def stylists(id: str, db: Session = Depends(get_db)):
    salon = db.query(Salon).filter(Salon.id == id).first()
    if not salon:
        raise HTTPException(status_code =404, detail="Salon not found")
    return db.query(Stylist).filter(Stylist.salon_id == id).all()

def finalize_bookings(db: Session, id: Optional[str] = None) -> List[Booking]:
    query = db.query(Booking).filter(Booking.status == "Upcoming", Booking.appointment_time < datetime.utcnow())

    if id:
        query = query.filter(Booking.user_id == id)
    stale_bookings = query.all()

    if not stale_bookings:
        return []
    for booking in stale_bookings:
        booking.status = "Done"
    db.commit()

    for booking in stale_bookings:
        db.refresh(booking)
        create_notification(db, booking.user_id, "How was your visit", f"Your appointment on {booking.appointment_time.strftime('%b %d %Y at %I%M %p')} is complete. We would love to hear how it went!", type="booking_done", r=booking.id)

    return stale_bookings