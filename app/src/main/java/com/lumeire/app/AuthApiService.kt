package com.lumeire.app

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/signup")
    suspend fun register(@Body request: SignUp): MessageResponse

    @POST("auth/verify")
    suspend fun verifyOtp(@Body request: Otp): TokenResponse

    @POST("auth/resend-otp")
    suspend fun resendOtp(@Body request: ResendOtp): MessageResponse

    @POST("auth/signin")
    suspend fun login(@Body request: SignIn): TokenResponse

    @POST("auth/google")
    suspend fun googleAuth(@Body request: GoogleAuth): TokenResponse

    @GET("auth/me")
    suspend fun getMe(): UserResponse
}