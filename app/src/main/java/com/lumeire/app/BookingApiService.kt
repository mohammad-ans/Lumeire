package com.lumeire.app

import com.lumeire.app.data.model.Booking
import com.lumeire.app.data.model.Salon
import com.lumeire.app.data.model.Service
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class BookingCreateRequest(
    val salon_id: String,
    val service_id: String,
    val stylist_id: String? = null,
    val appointment_time: String
)

interface BookingApiService {
    @GET("salons")
    suspend fun getSalons(): List<Salon>

    @GET("salons/{salon_id}/services")
    suspend fun getServices(@Path("salonId") salonId: String): List<Service>

    @POST("bookings")
    suspend fun createBooking(@Body request: BookingCreateRequest): Booking

    @GET("bookings")
    suspend fun getBookings(): List<Booking>

    @DELETE("bookings/{bookingId}")
    suspend fun cancelBooking(@Path("bookingId") bookingId: String): Booking
}