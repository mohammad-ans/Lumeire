import secrets
from datetime import datetime, timedelta
from typing import List, Optional
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from database import get_db
from models import Voucher, User, GiftCard
from schemas import VoucherResponse, ReferralInfoResponse
from auth import get_current_user

router = APIRouter()

FIRST_VISIT_DISCOUNT_PERCENT = 20.0
REFERRAL_REWARD_AMOUNT = 10.0

def first_visit_voucher(db: Session, user: User) -> Voucher:
    voucher = Voucher(
        user_id = user.id,
        code=f"FIRST20-{secrets.token_hex(3).upper()}",
        discount_type="percent",
        discount_value=FIRST_VISIT_DISCOUNT_PERCENT,
        reason="first_visit",
        expires_at=datetime.utcnow() + timedelta(days=30)
    )
    db.add(voucher)
    return voucher

def apply_referral_reward(db: Session, new_user: User, referred_by_code: Optional[str]) -> None:
    if not referred_by_code:
        return
    referrer = db.query(User).filter(User.referral_code == referred_by_code).first()
    if not referrer or referrer.id == new_user.id:
        return
    reward = GiftCard(
        salon_id = None,
        amount=REFERRAL_REWARD_AMOUNT,
        currency="USD",
        occasion="Referral Reward",
        message=f"{new_user.name or 'A friend'} joined Lustre using your referral code!",
        sender_id=new_user.id,
        receiver_id=referrer.id
    )
    db.add(reward)


def onboard_new_user(db: Session, user: User, referred_by_code: Optional[str] = None) -> None:
    first_visit_voucher(db, user)
    apply_referral_reward(db, user, referred_by_code)

@router.get("/vouchers/mine", response_model=List[VoucherResponse])
def get_vouchers(unused_only: bool = False, db: Session = Depends(get_db), user: User = Depends(get_current_user)):
    query = db.query(Voucher).filter(Voucher.user_id == user.id)
    if unused_only:
        query = query.filter(Voucher.is_used.is_(False))
    return query.order_by(Voucher.created_at.desc()).all()

@router.get("/referrals/mine", response_model=ReferralInfoResponse)
def get_my_referral_info(user: User = Depends(get_current_user)):
    return ReferralInfoResponse(
        referral_code=user.referral_code,
        share_message= f"Join Lustre using my code {user.referral_code} to get sign up rewards."
    )