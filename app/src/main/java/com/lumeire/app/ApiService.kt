package com.lumeire.app

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface ApiService {
    @GET("profile/me")
    suspend fun getMyProfile(): ProfileResponse

    @PUT("profile/me")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): ProfileResponse
}