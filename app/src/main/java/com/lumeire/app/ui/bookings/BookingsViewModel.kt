package com.lumeire.app.ui.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumeire.app.ApiClient
import com.lumeire.app.BookingReschedule
import com.lumeire.app.data.model.Booking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import okio.IOException
import retrofit2.HttpException

class BookingsViewModel : ViewModel() {

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchBookings()
    }
    fun refresh() {
        fetchBookings()
    }
    fun clearError() {
        _error.value = null
    }
    fun fetchBookings() {
        viewModelScope.launch {
            try {
                val result = ApiClient.bookingApiService.getBookings()
                _bookings.value = result
            } catch (e: Exception) {
                android.util.Log.e("BookingsViewModel", "Error fetching bookings", e)
                _error.value = toUserMessage(e)
            }
            finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelBooking(bookingId: String) {
     viewModelScope.launch {
         try {
             ApiClient.bookingApiService.cancelBooking(bookingId)
             fetchBookings()
         }
         catch (e: Exception){
             android.util.Log.e("BookingsViewModel", "Error cancelling bookings", e)
             _error.value = toUserMessage(e)
         }
     }
    }

    fun rescheduleBooking(id: String, time: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                ApiClient.bookingApiService.rescheduleBooking(id, BookingReschedule(appointment_time = time))
                fetchBookings()
                onResult(true, null)
            }
            catch (e: Exception) {
                val message = toUserMessage(e)
                _error.value = message
                onResult(false, message)
            }
        }
    }
    private fun toUserMessage(e: Exception): String {
        var message = ""
        when(e) {
            is HttpException -> {
                val code = e.code()
                message = when (code) {
                    401 -> "Please log in again"
                    404 -> "Not Found"
                    in 500..599 -> "Something went wrong on our end. Please try again."
                    else -> "Something went wrong (${code})"
                }
            }
            is IOException -> {
                message = "Couldn't connect. Check your internet connection."
            }
            else -> {
                message = "Something went wrong. Please try again."
            }
        }
        return message
    }
}