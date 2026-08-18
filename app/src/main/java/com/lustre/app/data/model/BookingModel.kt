package com.lustre.app.data.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Booking(
    val id: String,
    val user_id: String,
    val salon_id: String,
    val stylist_id: String? = null,
    val appointment_time: String,
    val status: String,
    val total_amount: Double,
    val currency: String = "USD",
    val payment_status: String,
    val payment_proof_url: String? = null,
    val created_at: String
)