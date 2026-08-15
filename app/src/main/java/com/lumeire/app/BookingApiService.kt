package com.lumeire.app

import com.lumeire.app.data.model.Booking
import com.lumeire.app.data.model.Salon
import com.lumeire.app.data.model.Service
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


interface BookingApiService {
    @GET("salons")
    suspend fun getSalons(@Query("category") category: String? = null, @Query("search") search: String? = null ): List<Salon>

    @GET("salons/{salon_id}")
    suspend fun getSalon(@Path("salon_id") salonId: String) : Salon
    @GET("salons/{salonId}/services")
    suspend fun getServices(@Path("salonId") salonId: String): List<Service>

    @POST("bookings")
    suspend fun createBooking(@Body request: BookingCreateRequest): Booking

    @GET("bookings")
    suspend fun getBookings(): List<Booking>

    @DELETE("bookings/{bookingId}")
    suspend fun cancelBooking(@Path("bookingId") bookingId: String): Booking

    @PATCH("bookings/{bookingId}/mark-paid")
    suspend fun markBookingPaid(@Path("bookingId") bookingId: String): Booking

    @GET("user/exists")
    suspend fun checkUser(@Query("email") email: String): EmailExistsResponse

    @POST("gifts")
    suspend fun sendGift(@Body request: GiftCardCreateRequest): GiftCard

    @GET("gifts/received")
    suspend fun getReceivedGifts(@Query("unused_only") unusedOnly: Boolean = true): List<GiftCard>

    @GET("gifts/sent")
    suspend fun getSendGifts(): List<GiftCard>
}