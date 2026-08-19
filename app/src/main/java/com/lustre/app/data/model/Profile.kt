package com.lustre.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val full_name: String? = null,
    val phone: String? = null,
    val date_of_birth: String? = null,
    val rewards_points: Int = 0,
    val fcm_token: String? = null,
)