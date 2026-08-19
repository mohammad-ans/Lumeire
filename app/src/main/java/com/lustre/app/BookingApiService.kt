package com.lustre.app

import com.lustre.app.data.model.Booking
import com.lustre.app.data.model.Salon
import com.lustre.app.data.model.Service
import com.lustre.app.data.model.Stylist
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query


interface BookingApiService {
    @GET("salons")
    suspend fun getSalons(@Query("category") category: String? = null, @Query("search") search: String? = null ): List<Salon>

    @GET("salons/{salon_id}")
    suspend fun getSalon(@Path("salon_id") salonId: String) : Salon
    @GET("salons/{salonId}/services")
    suspend fun getServices(@Path("salonId") salonId: String): List<Service>
    @GET("salons/{id}/stylists")
    suspend fun getStylists(@Path("id") id: String): List<Stylist>

    @POST("bookings")
    suspend fun createBooking(@Body request: BookingCreateRequest): Booking

    @GET("bookings")
    suspend fun getBookings(): List<Booking>

    @DELETE("bookings/{bookingId}")
    suspend fun cancelBooking(@Path("bookingId") bookingId: String): Booking

    @Multipart
    @POST("bookings/{id}/payment-proof")
    suspend fun uploadPayment(@Path("id") id: String, @Part file: MultipartBody.Part): Booking

    @PATCH("bookings/{id}/reschedule")
    suspend fun rescheduleBooking(@Path("id") id : String, @Body request: BookingReschedule): Booking
    @GET("user/exists")
    suspend fun checkUser(@Query("email") email: String): EmailExistsResponse

    @POST("gifts")
    suspend fun sendGift(@Body request: GiftCardCreateRequest): GiftCard

    @GET("gifts/received")
    suspend fun getReceivedGifts(@Query("unused_only") unusedOnly: Boolean = true): List<GiftCard>

    @GET("gifts/sent")
    suspend fun getSendGifts(): List<GiftCard>
}