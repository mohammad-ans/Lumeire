package com.lumeire.app.ui.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumeire.app.data.model.Booking
import com.lumeire.app.di.SupabaseModule
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import com.lumeire.app.data.model.Salon

class BookingsViewModel : ViewModel() {

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings

    init {
        fetchBookings()
    }
    fun refresh() {
        fetchBookings()
    }
    fun fetchSalons(onResult: (List<Salon>) -> Unit) {
        viewModelScope.launch {
            try {
                val result = SupabaseModule.client.postgrest["salons"]
                    .select(columns = Columns.ALL)
                    .decodeList<Salon>()
                onResult(result)
            } catch (e: Exception) {
                android.util.Log.e("BookingsViewModel", "Error fetching salons", e)
                onResult(emptyList())
            }
        }
    }
    private fun fetchBookings() {
        viewModelScope.launch {
            try {
                val session = SupabaseModule.client.auth.currentSessionOrNull()
                session?.user?.id?.let { userId ->
                    val result = SupabaseModule.client.postgrest["bookings"]
                        .select(columns = Columns.ALL) {
                            filter {
                                eq("user_id", userId)
                            }
                        }.decodeList<Booking>()
                    _bookings.value = result
                }
            } catch (e: Exception) {
                android.util.Log.e("BookingsViewModel", "Error fetching bookings", e)
            }
        }
    }
}