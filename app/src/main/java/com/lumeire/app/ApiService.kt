package com.lumeire.app

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface ApiService {
    @GET("profile/me")
    suspend fun getMyProfile(): ProfileResponse

    @PUT("profile/me")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): ProfileResponse

    @Multipart
    @POST("profile/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): ProfileResponse
}