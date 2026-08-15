package com.lumeire.app

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {
    @GET("profile/me")
    suspend fun getMyProfile(): ProfileResponse

    @PUT("profile/me")
    suspend fun updateProfile(@Body request: ProfileUpdateRequest): ProfileResponse

    @Multipart
    @POST("profile/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): ProfileResponse

    @PUT("profile/me/password")
    suspend fun changePassword(@Body request: PasswordChangeReq) : MessageResponse

    @HTTP(method = "DELETE", path = "profile/me")
    suspend fun deleteAccount(): MessageResponse

    @POST("support/tickets")
    suspend fun createSupportTicket(@Body request: SupportTicketCreateReq) : SupportTicket

    @GET("support/tickets")
    suspend fun getTickets(): List<SupportTicket>

    @GET("notifications")
    suspend fun getNotifications(): List<Notification>

    @GET("notifications/unreadCount")
    suspend fun getUnread(): Boolean

    @PATCH("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String): Notification

    @PATCH("notifications/readAll")
    suspend fun markAllRead(): MessageResponse
}