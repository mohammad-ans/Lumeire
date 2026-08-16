from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
import asyncio
from database import engine, Base, SessionLocal
import models
import auth
from profile import router
import bookings
import gift
import support
import vouchers
import notifications

Base.metadata.create_all(bind=engine)

app = FastAPI()
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
    allow_credentials=False
)

app.mount("/static", StaticFiles(directory="static"), name="static")

app.include_router(auth.router)
app.include_router(router)
app.include_router(bookings.router)
app.include_router(gift.router)
app.include_router(support.router)
app.include_router(vouchers.router)
app.include_router(notifications.router)

async def finalize_bookings_loop():
    while True:
        try:
            db = SessionLocal()
            try:
                bookings.finalize_bookings(db)
            finally:
                db.close()
        finally:
            await asyncio.sleep(600)

@app.on_event("startup")
async def bg_task():
    asyncio.create_task(finalize_bookings_loop())