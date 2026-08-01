package com.lumeire.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Salon(
    val id: String,
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rating: Double? = null,
    val review_count: Int = 0,
    val phone: String? = null,
    val website: String? = null,
    val openTime: String? = null,
    val closeTime: String? = null,
    val image_url: String? = null
)