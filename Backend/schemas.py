from typing import Optional
from pydantic import BaseModel, EmailStr, ConfigDict

class SignUp(BaseModel):
    email: EmailStr
    password: str
    name: str

class SignIn(BaseModel):
    email: EmailStr
    password: str

class GoogleAuth(BaseModel):
    id_token: str

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"

class UserResponse(BaseMode):
    id: str
    email: str
    name: Optional[str]  = None
    auth_provide: str
    is_verified: str
    model_config = ConfigDict(from_attributes=True)