package com.lustre.app

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class VoucherResponse(
    val id: String,
    val code: String,
    val discount_type: String,
    val discount_value: Double,
    val reason: String? = null,
    val is_used: Boolean,
    val expires_at: String? = null,
    val created_at: String
)

@Serializable
data class ReferralInforesponse(
    val referral_code: String,
    val share_message: String
)

interface VoucherApiService {
    @GET("vouchers/mine")
    suspend fun getMyVouchers(@Query("unused_only") unusedOnly: Boolean = false): List<VoucherResponse>

    @GET("referrals/mine")
    suspend fun getMyReferralInfo(): ReferralInforesponse
}