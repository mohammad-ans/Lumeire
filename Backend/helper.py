from datetime import datetime, timedelta
from passlib.context import CryptContext

from database import SessionLocal
from models import (
    User, Profile, Salon, Service, Stylist, Booking,
    GiftCard, Voucher, SupportTicket, Notification, gen_uuid
)

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
SEED_PASSWORD = "Test@1234"
HASHED_PASSWORD = pwd_context.hash(SEED_PASSWORD)

now = datetime.utcnow()
db = SessionLocal()

# ---------------------------------------------------------------------------
# IDs generated up front so every table below can reference the right row
# without depending on SQLAlchemy's flush/commit timing.
# ---------------------------------------------------------------------------

# Users
id_gizzy, id_sarah, id_ali, id_fatima, id_hamza = (gen_uuid() for _ in range(5))

# Salons
id_salon_comb, id_salon_bloom, id_salon_velvet, id_salon_radiance, id_salon_grooming = (gen_uuid() for _ in range(5))

# Services
id_svc_womens_cut, id_svc_mens_cut, id_svc_color, id_svc_keratin = (gen_uuid() for _ in range(4))
id_svc_swedish, id_svc_deep_tissue, id_svc_hot_stone, id_svc_aroma_wrap = (gen_uuid() for _ in range(4))
id_svc_manicure, id_svc_gel_pedicure, id_svc_acrylic, id_svc_nail_art = (gen_uuid() for _ in range(4))
id_svc_hydrating_facial, id_svc_antiaging_facial, id_svc_chemical_peel, id_svc_microderm = (gen_uuid() for _ in range(4))
id_svc_classic_cut, id_svc_beard_trim, id_svc_hot_shave, id_svc_cut_beard_combo = (gen_uuid() for _ in range(4))

# Stylists
id_stylist_emma, id_stylist_jason = (gen_uuid() for _ in range(2))
id_stylist_maria, id_stylist_nina = (gen_uuid() for _ in range(2))
id_stylist_chloe, id_stylist_ava = (gen_uuid() for _ in range(2))
id_stylist_priya, id_stylist_sofia = (gen_uuid() for _ in range(2))
id_stylist_marcus, id_stylist_diego = (gen_uuid() for _ in range(2))

# Bookings
id_booking_ans_haircut = gen_uuid()
id_booking_ans_massage_pending = gen_uuid()
id_booking_ans_facial_done = gen_uuid()
id_booking_sarah_pedicure = gen_uuid()
id_booking_sarah_color_done = gen_uuid()
id_booking_ali_combo = gen_uuid()
id_booking_fatima_hotstone_done = gen_uuid()
id_booking_hamza_facial_cancelled = gen_uuid()
id_booking_hamza_manicure = gen_uuid()

# ---------------------------------------------------------------------------
# 1. Users
# ---------------------------------------------------------------------------
users = [
    User(id=id_gizzy, email="ansmuhammad098@gmail.com", name="Muhammad Ans",
         hashed_password=HASHED_PASSWORD, auth_provider="local", verified=True,
         referral_code="ANS2026", created_at=now - timedelta(days=120)),
    User(id=id_sarah, email="sarah.khan@example.com", name="Sarah Khan",
         hashed_password=HASHED_PASSWORD, auth_provider="local", verified=True,
         referral_code="SARAH88", created_at=now - timedelta(days=95)),
    User(id=id_ali, email="ali.raza@example.com", name="Ali Raza",
         hashed_password=HASHED_PASSWORD, auth_provider="local", verified=True,
         referral_code="ALIRZ42", created_at=now - timedelta(days=80)),
    User(id=id_fatima, email="fatima.noor@example.com", name="Fatima Noor",
         hashed_password=HASHED_PASSWORD, auth_provider="local", verified=True,
         referral_code="FATI2019", created_at=now - timedelta(days=60)),
    User(id=id_hamza, email="hamza.tariq@example.com", name="Hamza Tariq",
         hashed_password=HASHED_PASSWORD, auth_provider="local", verified=True,
         referral_code="HAMZA777", created_at=now - timedelta(days=10)),
]
db.add_all(users)
db.commit()

# ---------------------------------------------------------------------------
# 2. Profiles  (reward_points deliberately spread across low/mid/high to
#    exercise different loyalty tiers; avatar_url = placeholder portrait)
# ---------------------------------------------------------------------------
profiles = [
    Profile(id=id_gizzy, full_name="Muhammad Ans", phone="+1 415-555-0132", dob="1998-04-12",
            reward_points=680, avatar_url="https://randomuser.me/api/portraits/men/32.jpg"),
    Profile(id=id_sarah, full_name="Sarah Khan", phone="+1 415-555-0187", dob="1995-09-23",
            reward_points=1450, avatar_url="https://randomuser.me/api/portraits/women/44.jpg"),
    Profile(id=id_ali, full_name="Ali Raza", phone="+1 415-555-0214", dob="1992-01-05",
            reward_points=210, avatar_url="https://randomuser.me/api/portraits/men/56.jpg"),
    Profile(id=id_fatima, full_name="Fatima Noor", phone="+1 415-555-0298", dob="1999-11-30",
            reward_points=95, avatar_url="https://randomuser.me/api/portraits/women/68.jpg"),
    Profile(id=id_hamza, full_name="Hamza Tariq", phone="+1 415-555-0355", dob="1997-06-18",
            reward_points=40, avatar_url="https://randomuser.me/api/portraits/men/71.jpg"),
]
db.add_all(profiles)
db.commit()

# ---------------------------------------------------------------------------
# 3. Salons  (categories deliberately contain "Haircut"/"Massage"/"Nails"/
#    "Facial" so Home's category chip filter - which does a substring
#    match - actually returns results for each chip)
# ---------------------------------------------------------------------------
salons = [
    Salon(id=id_salon_comb, name="The Golden Comb", category="Haircut & Styling",
          address="214 Post St, Union Square, San Francisco, CA", latitude=37.7879, longitude=-122.4075,
          rating=4.7, review_count=286, phone="+1 415-555-1001", website="https://thegoldencomb.example.com",
          openTime="9:00 AM", closeTime="7:00 PM",
          image_url="https://picsum.photos/seed/golden-comb-salon/800/600", currency="USD"),
    Salon(id=id_salon_bloom, name="Bloom & Glow Spa", category="Massage & Spa",
          address="88 Hayes St, Hayes Valley, San Francisco, CA", latitude=37.7759, longitude=-122.4245,
          rating=4.9, review_count=412, phone="+1 415-555-1002", website="https://bloomandglow.example.com",
          openTime="10:00 AM", closeTime="8:00 PM",
          image_url="https://picsum.photos/seed/bloom-glow-spa/800/600", currency="USD"),
    Salon(id=id_salon_velvet, name="Velvet Nails Studio", category="Nails & Beauty",
          address="1502 Valencia St, Mission District, San Francisco, CA", latitude=37.7599, longitude=-122.4148,
          rating=4.5, review_count=198, phone="+1 415-555-1003", website="https://velvetnails.example.com",
          openTime="9:30 AM", closeTime="6:30 PM",
          image_url="https://picsum.photos/seed/velvet-nails-studio/800/600", currency="USD"),
    Salon(id=id_salon_radiance, name="Radiance Skin Clinic", category="Facial & Skincare",
          address="2100 Chestnut St, Marina District, San Francisco, CA", latitude=37.8030, longitude=-122.4377,
          rating=4.8, review_count=331, phone="+1 415-555-1004", website="https://radianceskin.example.com",
          openTime="9:00 AM", closeTime="6:00 PM",
          image_url="https://picsum.photos/seed/radiance-skin-clinic/800/600", currency="USD"),
    Salon(id=id_salon_grooming, name="The Grooming Lounge", category="Haircut & Styling",
          address="1980 Union St, Pacific Heights, San Francisco, CA", latitude=37.7925, longitude=-122.4382,
          rating=4.6, review_count=154, phone="+1 415-555-1005", website="https://groominglounge.example.com",
          openTime="10:00 AM", closeTime="8:00 PM",
          image_url="https://picsum.photos/seed/grooming-lounge-barber/800/600", currency="USD"),
]
db.add_all(salons)
db.commit()

# ---------------------------------------------------------------------------
# 4. Services
# ---------------------------------------------------------------------------
services = [
    # The Golden Comb
    Service(id=id_svc_womens_cut, name="Women's Haircut & Blowout", category="Haircut",
            duration_minutes=60, price=65.0, salon_id=id_salon_comb),
    Service(id=id_svc_mens_cut, name="Men's Haircut", category="Haircut",
            duration_minutes=30, price=35.0, salon_id=id_salon_comb),
    Service(id=id_svc_color, name="Full Color & Highlights", category="Coloring",
            duration_minutes=120, price=180.0, salon_id=id_salon_comb),
    Service(id=id_svc_keratin, name="Keratin Treatment", category="Treatment",
            duration_minutes=150, price=220.0, salon_id=id_salon_comb),

    # Bloom & Glow Spa
    Service(id=id_svc_swedish, name="Swedish Massage (60 min)", category="Massage",
            duration_minutes=60, price=95.0, salon_id=id_salon_bloom),
    Service(id=id_svc_deep_tissue, name="Deep Tissue Massage", category="Massage",
            duration_minutes=90, price=130.0, salon_id=id_salon_bloom),
    Service(id=id_svc_hot_stone, name="Hot Stone Therapy", category="Massage",
            duration_minutes=75, price=110.0, salon_id=id_salon_bloom),
    Service(id=id_svc_aroma_wrap, name="Aromatherapy Body Wrap", category="Spa",
            duration_minutes=60, price=100.0, salon_id=id_salon_bloom),

    # Velvet Nails Studio
    Service(id=id_svc_manicure, name="Classic Manicure", category="Nails",
            duration_minutes=30, price=30.0, salon_id=id_salon_velvet),
    Service(id=id_svc_gel_pedicure, name="Gel Pedicure", category="Nails",
            duration_minutes=45, price=50.0, salon_id=id_salon_velvet),
    Service(id=id_svc_acrylic, name="Acrylic Full Set", category="Nails",
            duration_minutes=75, price=70.0, salon_id=id_salon_velvet),
    Service(id=id_svc_nail_art, name="Nail Art Add-On", category="Nails",
            duration_minutes=20, price=15.0, salon_id=id_salon_velvet),

    # Radiance Skin Clinic
    Service(id=id_svc_hydrating_facial, name="Signature Hydrating Facial", category="Facial",
            duration_minutes=60, price=85.0, salon_id=id_salon_radiance),
    Service(id=id_svc_antiaging_facial, name="Anti-Aging Facial", category="Facial",
            duration_minutes=75, price=120.0, salon_id=id_salon_radiance),
    Service(id=id_svc_chemical_peel, name="Chemical Peel", category="Facial",
            duration_minutes=45, price=150.0, salon_id=id_salon_radiance),
    Service(id=id_svc_microderm, name="Microdermabrasion", category="Facial",
            duration_minutes=50, price=135.0, salon_id=id_salon_radiance),

    # The Grooming Lounge
    Service(id=id_svc_classic_cut, name="Classic Men's Cut", category="Haircut",
            duration_minutes=30, price=40.0, salon_id=id_salon_grooming),
    Service(id=id_svc_beard_trim, name="Beard Trim & Shape-Up", category="Grooming",
            duration_minutes=20, price=25.0, salon_id=id_salon_grooming),
    Service(id=id_svc_hot_shave, name="Hot Towel Shave", category="Grooming",
            duration_minutes=30, price=45.0, salon_id=id_salon_grooming),
    Service(id=id_svc_cut_beard_combo, name="Cut + Beard Combo", category="Haircut",
            duration_minutes=45, price=60.0, salon_id=id_salon_grooming),
]
db.add_all(services)
db.commit()

# ---------------------------------------------------------------------------
# 5. Stylists
# ---------------------------------------------------------------------------
stylists = [
    Stylist(id=id_stylist_emma, name="Emma Rodriguez", speciality="Color Specialist", salon_id=id_salon_comb),
    Stylist(id=id_stylist_jason, name="Jason Lee", speciality="Precision Cuts", salon_id=id_salon_comb),

    Stylist(id=id_stylist_maria, name="Maria Gonzalez", speciality="Deep Tissue & Sports Massage", salon_id=id_salon_bloom),
    Stylist(id=id_stylist_nina, name="Nina Patel", speciality="Aromatherapy", salon_id=id_salon_bloom),

    Stylist(id=id_stylist_chloe, name="Chloe Tran", speciality="Nail Art & Design", salon_id=id_salon_velvet),
    Stylist(id=id_stylist_ava, name="Ava Kim", speciality="Gel & Acrylics", salon_id=id_salon_velvet),

    Stylist(id=id_stylist_priya, name="Dr. Priya Sharma", speciality="Clinical Skincare", salon_id=id_salon_radiance),
    Stylist(id=id_stylist_sofia, name="Sofia Martinez", speciality="Anti-Aging Treatments", salon_id=id_salon_radiance),

    Stylist(id=id_stylist_marcus, name="Marcus Johnson", speciality="Classic Barbering", salon_id=id_salon_grooming),
    Stylist(id=id_stylist_diego, name="Diego Alvarez", speciality="Beard Grooming", salon_id=id_salon_grooming),
]
db.add_all(stylists)
db.commit()

# ---------------------------------------------------------------------------
# 6. Bookings
#    Covers every state your UI branches on: Upcoming/unpaid, Upcoming with
#    a proof already submitted (pending_verification - hides "Pay Now"),
#    Done/paid, Cancelled/refund_due, and a stylist_id=None ("no preference") case.
# ---------------------------------------------------------------------------
bookings = [
    # Upcoming, unpaid, with stylist chosen
    Booking(id=id_booking_ans_haircut, user_id=id_gizzy, salon_id=id_salon_comb, stylist_id=id_stylist_emma,
            appointment_time=now + timedelta(days=3, hours=2), status="Upcoming",
            total_amount=65.0, currency="USD", payment_status="unpaid",
            created_at=now - timedelta(days=1)),

    # Upcoming, payment proof already uploaded -> pending_verification
    Booking(id=id_booking_ans_massage_pending, user_id=id_gizzy, salon_id=id_salon_bloom, stylist_id=id_stylist_maria,
            appointment_time=now + timedelta(days=6), status="Upcoming",
            total_amount=130.0, currency="USD", payment_status="pending_verification",
            payment_proof_url="https://picsum.photos/seed/payment-proof-receipt-1/600/800",
            created_at=now - timedelta(hours=5)),

    # Done, paid (past appointment)
    Booking(id=id_booking_ans_facial_done, user_id=id_gizzy, salon_id=id_salon_radiance, stylist_id=id_stylist_priya,
            appointment_time=now - timedelta(days=20), status="Done",
            total_amount=85.0, currency="USD", payment_status="paid",
            created_at=now - timedelta(days=22)),

    # Upcoming, unpaid, stylist_id=None ("no preference" path)
    Booking(id=id_booking_sarah_pedicure, user_id=id_sarah, salon_id=id_salon_velvet, stylist_id=None,
            appointment_time=now + timedelta(days=2, hours=4), status="Upcoming",
            total_amount=50.0, currency="USD", payment_status="unpaid",
            created_at=now - timedelta(hours=12)),

    # Done, paid - this is the booking the LOYAL20 voucher (20% off $180) was redeemed against
    Booking(id=id_booking_sarah_color_done, user_id=id_sarah, salon_id=id_salon_comb, stylist_id=id_stylist_emma,
            appointment_time=now - timedelta(days=35), status="Done",
            total_amount=144.0, currency="USD", payment_status="paid",
            created_at=now - timedelta(days=37)),

    # Upcoming, unpaid
    Booking(id=id_booking_ali_combo, user_id=id_ali, salon_id=id_salon_grooming, stylist_id=id_stylist_marcus,
            appointment_time=now + timedelta(days=1, hours=3), status="Upcoming",
            total_amount=60.0, currency="USD", payment_status="unpaid",
            created_at=now - timedelta(hours=8)),

    # Done, paid $10 - this is the booking the $100 gift card (against a $110 service) was redeemed against
    Booking(id=id_booking_fatima_hotstone_done, user_id=id_fatima, salon_id=id_salon_bloom, stylist_id=id_stylist_nina,
            appointment_time=now - timedelta(days=15), status="Done",
            total_amount=10.0, currency="USD", payment_status="paid",
            created_at=now - timedelta(days=18)),

    # Cancelled after being paid -> refund_due (matches cancel_booking's actual logic)
    Booking(id=id_booking_hamza_facial_cancelled, user_id=id_hamza, salon_id=id_salon_radiance, stylist_id=id_stylist_sofia,
            appointment_time=now + timedelta(days=4), status="Cancelled",
            total_amount=120.0, currency="USD", payment_status="refund_due",
            created_at=now - timedelta(days=3)),

    # Upcoming, unpaid
    Booking(id=id_booking_hamza_manicure, user_id=id_hamza, salon_id=id_salon_velvet, stylist_id=id_stylist_chloe,
            appointment_time=now + timedelta(days=5, hours=1), status="Upcoming",
            total_amount=30.0, currency="USD", payment_status="unpaid",
            created_at=now - timedelta(hours=2)),
]
db.add_all(bookings)
db.commit()

# ---------------------------------------------------------------------------
# 7. Gift Cards
#    #1 is unredeemed (testable in the booking flow's gift-card prompt).
#    #2 is redeemed - amount matches booking #id_booking_fatima_hotstone_done
#       exactly ($110 service - $100 gift card = $10 total_amount above).
#    #3 simulates a referral reward (Hamza signed up using Ans's code).
# ---------------------------------------------------------------------------
gift_cards = [
    GiftCard(id=gen_uuid(), salon_id=id_salon_comb, service_id=None, amount=50.0, currency="USD",
             occasion="Birthday", message="Happy Birthday! Treat yourself \U0001F389",
             sender_id=id_gizzy, receiver_id=id_sarah, is_used=False,
             created_at=now - timedelta(days=4)),

    GiftCard(id=gen_uuid(), salon_id=None, service_id=None, amount=100.0, currency="USD",
             occasion="Thank You", message="Thanks for everything - enjoy on me!",
             sender_id=id_ali, receiver_id=id_fatima, is_used=True,
             redeemed_booking_id=id_booking_fatima_hotstone_done,
             created_at=now - timedelta(days=19)),

    GiftCard(id=gen_uuid(), salon_id=None, service_id=None, amount=15.0, currency="USD",
             occasion="Referral", message="Referral reward - thanks for inviting a friend!",
             sender_id=id_hamza, receiver_id=id_gizzy, is_used=False,
             created_at=now - timedelta(days=10)),
]
db.add_all(gift_cards)
db.commit()

# ---------------------------------------------------------------------------
# 8. Vouchers
#    LOYAL20 is redeemed against booking id_booking_sarah_color_done
#    ($180 service x 20% off = $144, matches that booking's total_amount).
#    SUMMER15 is deliberately already expired, to exercise that check.
# ---------------------------------------------------------------------------
vouchers = [
    Voucher(id=gen_uuid(), user_id=id_gizzy, code="WELCOME10", discount_type="percent", discount_value=10.0,
            reason="New user welcome bonus", is_used=False,
            expires_at=now + timedelta(days=60), created_at=now - timedelta(days=120)),

    Voucher(id=gen_uuid(), user_id=id_sarah, code="LOYAL20", discount_type="percent", discount_value=20.0,
            reason="Loyalty reward", is_used=True, redeemed_booking_id=id_booking_sarah_color_done,
            expires_at=now + timedelta(days=10), created_at=now - timedelta(days=40)),

    Voucher(id=gen_uuid(), user_id=id_ali, code="SUMMER15", discount_type="percent", discount_value=15.0,
            reason="Summer promo", is_used=False,
            expires_at=now - timedelta(days=5), created_at=now - timedelta(days=95)),  # already expired

    Voucher(id=gen_uuid(), user_id=id_fatima, code="FLAT25OFF", discount_type="fixed", discount_value=25.0,
            reason="Referral bonus", is_used=False,
            expires_at=now + timedelta(days=30), created_at=now - timedelta(days=5)),
]
db.add_all(vouchers)
db.commit()

# ---------------------------------------------------------------------------
# 9. Support Tickets
# ---------------------------------------------------------------------------
tickets = [
    SupportTicket(id=gen_uuid(), user_id=id_gizzy, subject="Unable to reschedule booking",
                  message="I'm trying to reschedule my haircut appointment but the app keeps freezing on the date picker.",
                  status="open", created_at=now - timedelta(days=2)),
    SupportTicket(id=gen_uuid(), user_id=id_sarah, subject="Refund not received",
                  message="My massage booking was cancelled two weeks ago and I still haven't received the refund.",
                  status="open", created_at=now - timedelta(days=6)),
    SupportTicket(id=gen_uuid(), user_id=id_ali, subject="App crashes on payment screen",
                  message="Every time I try to upload my payment screenshot the app closes unexpectedly.",
                  status="resolved", created_at=now - timedelta(days=14)),
]
db.add_all(tickets)
db.commit()

# ---------------------------------------------------------------------------
# 10. Notifications  (each one references the real booking/event it's about)
# ---------------------------------------------------------------------------
notifications = [
    Notification(id=gen_uuid(), user_id=id_gizzy, title="Booking confirmed",
                 body="Your appointment at The Golden Comb has been booked.",
                 type="booking", related_booking_id=id_booking_ans_haircut, is_read=False,
                 created_at=now - timedelta(days=1)),

    Notification(id=gen_uuid(), user_id=id_gizzy, title="Payment proof submitted",
                 body="We're reviewing your payment proof for your upcoming appointment at Bloom & Glow Spa.",
                 type="booking", related_booking_id=id_booking_ans_massage_pending, is_read=True,
                 created_at=now - timedelta(hours=5)),

    Notification(id=gen_uuid(), user_id=id_sarah, title="Booking confirmed",
                 body="Your appointment at Velvet Nails Studio has been booked.",
                 type="booking", related_booking_id=id_booking_sarah_pedicure, is_read=False,
                 created_at=now - timedelta(hours=12)),

    Notification(id=gen_uuid(), user_id=id_hamza, title="Booking cancelled",
                 body="Your appointment at Radiance Skin Clinic has been cancelled.",
                 type="booking", related_booking_id=id_booking_hamza_facial_cancelled, is_read=True,
                 created_at=now - timedelta(days=3)),

    Notification(id=gen_uuid(), user_id=id_sarah, title="You've received a gift! \U0001F381",
                 body="Muhammad Ans sent you a $50 gift card for The Golden Comb.",
                 type="gift", related_booking_id=None, is_read=False,
                 created_at=now - timedelta(days=4)),

    Notification(id=gen_uuid(), user_id=id_gizzy, title="Referral reward earned!",
                 body="Hamza Tariq joined Lumeire using your referral code! You've received a reward.",
                 type="referral", related_booking_id=None, is_read=True,
                 created_at=now - timedelta(days=10)),
]
db.add_all(notifications)
db.commit()
db.flush()

print("Seed data inserted successfully.")
print(f"All seeded users log in with password: {SEED_PASSWORD}")
for u in users:
    print(f"  - {u.email}")
db.close()