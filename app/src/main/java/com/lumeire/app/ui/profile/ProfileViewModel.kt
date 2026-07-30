package com.lumeire.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumeire.app.ApiClient
import com.lumeire.app.ProfileResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _profile = MutableStateFlow<ProfileResponse?>(null)
    val profile: StateFlow<ProfileResponse?> = _profile
    
    private val _totalBookings = MutableStateFlow(0)
    val totalBookings: StateFlow<Int> = _totalBookings

    init {
        fetchProfile()
    }

    fun refresh() = fetchProfile()
    private fun fetchProfile() {
        viewModelScope.launch {
            try {
                val result = ApiClient.apiService.getMyProfile()
                _profile.value = result
                _totalBookings.value = result.total_bookings

            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error fetching profile", e)
            }
        }
    }
}
