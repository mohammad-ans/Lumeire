package com.lumeire.app.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumeire.app.data.model.Profile
import com.lumeire.app.di.SupabaseModule
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile
    
    private val _totalBookings = MutableStateFlow(0)
    val totalBookings: StateFlow<Int> = _totalBookings

    init {
        fetchProfile()
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            try {
                val session = SupabaseModule.client.auth.currentSessionOrNull()
                session?.user?.id?.let { userId ->
                    val result = SupabaseModule.client.postgrest["profiles"]
                        .select(columns = Columns.ALL) {
                            filter {
                                eq("id", userId)
                            }
                        }.decodeSingleOrNull<Profile>()
                    _profile.value = result
                    Log.d("Profile", "Received: $result")
                    val bookingsResult = SupabaseModule.client.postgrest["bookings"]
                        .select(columns = Columns.ALL) {
                            filter {
                                eq("user_id", userId)
                            }
                        }.decodeList<com.lumeire.app.data.model.Booking>()
                    _totalBookings.value = bookingsResult.size
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error fetching profile", e)
            }
        }
    }
}
