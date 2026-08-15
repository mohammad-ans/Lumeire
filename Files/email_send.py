import os
import httpx
from dotenv import load_dotenv


load_dotenv()
API_KEY = os.getenv("RESEND_API_KEY")
RESEND_EMAIL = os.getenv("RESEND_EMAIL")

async def send_otp(email: str, otp: str):
    url = "https://api.resend.com/emails"
    header = {
        "Authorization" : f"Bearer {API_KEY}",
        "Content-Type" : "application/json"
    }
    body = {
    "from" : RESEND_EMAIL,
    "to" : email,
    "subject" : "Lumeire Verification Code",
    "html" : (
        f"<p>Your verification code is: <strong>{otp}</strong></p>"
        f"<p>It expires in 10 minutes.</p>"
    )
    }
    async with httpx.AsyncClient() as client:
        response = await client.post(url, json=body, headers = header)
        return response.status_code
