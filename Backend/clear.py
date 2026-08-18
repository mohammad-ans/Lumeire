from database import Base, engine, SessionLocal
from models import User, PendingUser, Profile, GiftCard, Salon, Service, Stylist, SupportTicket, PasswordReset, Voucher, Booking, Notification


Base.metadata.create_all(bind=engine)
db = SessionLocal()
db.query(SupportTicket).delete()
db.query(PendingUser).delete()
db.query(Profile).delete()
db.query(PasswordReset).delete()
db.query(Voucher).delete()
db.query(Notification).delete()
db.query(GiftCard).delete()
db.query(Service).delete()
db.query(Booking).delete()
db.query(Stylist).delete()
db.query(Salon).delete()
db.query(User).delete()

db.commit()


# PasswordReset.__table__.drop(engine)
# SupportTicket.__table__.drop(engine)
# Profile.__table__.drop(engine)
# PendingUser.__table__.drop(engine)
# Voucher.__table__.drop(engine)
# Notification.__table__.drop(engine)
# GiftCard.__table__.drop(engine)
# Booking.__table__.drop(engine)
# Service.__table__.drop(engine)
# Stylist.__table__.drop(engine)
# Salon.__table__.drop(engine)
# User.__table__.drop(engine)