package com.lumeire.app

import kotlinx.serialization.Serializable


@Serializable
data class SignUp(
    val email: String,
    val password: String,
    val full_name: String
)

@Serializable
data class SignIn(
    val email: String,
    val password: String
)

@Serializable
data class GoogleAuth(
    val id_token: String
)

@Serializable
data class Otp(
    val email: String,
    val otp_code: String
)

@Serializable
data class ResendOtp(
    val email: String
)

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String = "bearer"
)

@Serializable
data class MessageResponse(
    val message: String
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val name: String? = null,
    val auth_provider: String,
    val is_verified: Boolean
)

@Serializable
data class ErrorResponse(
    val detail: String
)