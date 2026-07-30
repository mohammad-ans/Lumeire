from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from database import get_db
from models import Booking, Salon, Service, Stylist, User
from schemas import BookingCreateRequest, BookingResponse, SalonResponse, ServiceResponse

from auth import get_current_user

router = APIRouter(prefix="/bookings")

@router.get("/salons", response_model=List[SalonResponse])
def list_salons(db: Session = Depends(get_db)):
    return db.query(Salon).all()

@router.get("/salons/{salon_id}/services", response_model=List[ServiceResponse])
def list_services(salon_id: str, db: Session = Depends(get_db)):
    salon = db.query(Salon).filter(Salon.id == salon_id).first()
    if not salon:
        raise HTTPException(status_code=404, detail="Salon Not Found")
    return db.query(Service).filter(Service.salon_id == salon_id).all()


@router.post("/bookings", response_model=BookingResponse, status_code=201)
def create_booking(data: BookingCreateRequest, db: Session = Depends(get_db), current_user: Depends()get_current_user)):
    service = db.query(Service).filter(Service.id == data.service_id, Service.salon_id == data.salon_id).first()
    if not service:
        raise HTTPException(status_code=404, detail="Service not found for this salon")
    if data.stylist_id:
        stylist = db.query(Stylist).filter(Stylist.id == data.stylist_id, Stylist.salon_id == data.salon_id).first()
        if not stylist:
            raise HTTPException(status_code=404, detail="Stylist not found for this salon")

    booking = Booking(
    user_id = current_user.id,
    salon_id = data.salon_id,
    stylist_id = data.stylist_id,
    appointment_time = data.appointment_time,
    status = "Upcoming",
    total_amount = service.price
    )
    db.add(booking)
    db.commit()
    db.refresh(booking)
    return booking

@router.get("/bookings", response_model=List[BookingResponse])
def list_bookings(db : Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    return db.query(Booking).filter(Booking.user_id == current_user.id).order_by(Booking.appointment_time.desc()).all()

@router.delete("/bookings/{booking_id}", response_model=BookingResponse)
def cancel_booking(booking_id: str, db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    booking = db.query(Booking).filter(Booking.id == booking_id, Booking.user_id == current_user.id).first()
    if not booking:
        raise HTTPException(status_code=404, detail="Booking Not found")
    if booking.status == "Cancelled":
        raise HTTPException(status_code=400, detail="Booking is already cancelled")

    booking.status = "Cancelled"
    db.commit()
    db.refresh(booking)
    return booking