package com.lumeire.app

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SignUp(
    val email: String,
    val password: String,
    val full_name: String
)
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class SignIn(
    val email: String,
    val password: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GoogleAuth(
    val id_token: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Otp(
    val email: String,
    val otp_code: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ResendOtp(
    val email: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String = "bearer"
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MessageResponse(
    val message: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val name: String? = null,
    val auth_provider: String,
    val is_verified: Boolean
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ErrorResponse(
    val detail: String
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ProfileResponse(
    val id: String,
    val email: String,
    val full_name: String? = null,
    val phone: String? = null,
    val data_of_birth: String? = null,
    val reward_points: Int = 0,
    val fcm_token: String? = null,
    val avatar_url: String? = null,
    val total_bookings: Int = 0,
    val loyalty_tier: String = "Bronze",
    val next_tier: String? = null,
    val points_next_tier: Int? = null,
    val tier_progress: Double = 0.0
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ProfileUpdateRequest(
    val full_name: String? = null,
    val phone: String? = null,
    val date_of_birth: String? = null,
    val fcm_token: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GiftCard(
    val id: String,
    val salon_id: String,
    val service_id: String? = null,
    val amount: Double,
    val currency: String = "USD",
    val occasion: String? = null,
    val message: String? = null,
    val sender_id: String,
    val receiver_id: String,
    val is_used: Boolean,
    val created_at: String,
    val salon_name: String? = null,
    val sender_name: String? = null,
    val receiver_name: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class BookingCreateRequest(
    val salon_id: String,
    val service_id: String,
    val stylist_id: String? = null,
    val appointment_time: String,
    val gift_card_id: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class GiftCardCreateRequest(
    val receiver_email: String,
    val salon_id: String,
    val service_id: String? = null,
    val amount: Double? = null,
    val occasion: String? = null,
    val message: String? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class EmailExistsResponse(val exists: Boolean)

@SuppressLint("UnsafeOptInUsageError")
data class ForgotPassword(val email: String)

@SuppressLint("UnsafeOptInUsageError")
data class ResetPassword(
    val email: String,
    val otp_code: String,
    val new_password: String
)

data class PasswordChangeReq(
    val curr_pass: String,
    val new_pass: String
)

data class SupportTicketCreateReq(
    val sbj: String,
    val msg: String
)

data class SupportTicket(
    val id: String,
    val sbj: String,
    val msg: String,
    val status: String,
    val created_at: String
)