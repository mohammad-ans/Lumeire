// data/model/Service.kt
package com.lumeire.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Service(
    val id: String,
    val name: String,
    val category: String,
    val duration_minutes: Int,
    val price: Double,
    val salon_id: String,
    val currency: String = "USD"
)