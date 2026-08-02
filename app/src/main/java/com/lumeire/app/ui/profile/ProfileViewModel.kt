package com.lumeire.app.ui.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumeire.app.ApiClient
import com.lumeire.app.GiftCard
import com.lumeire.app.ProfileResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URI

class ProfileViewModel : ViewModel() {

    private val _profile = MutableStateFlow<ProfileResponse?>(null)
    val profile: StateFlow<ProfileResponse?> = _profile
    
    private val _totalBookings = MutableStateFlow(0)
    val totalBookings: StateFlow<Int> = _totalBookings

    private val _avatarError = MutableStateFlow<String?>(null)
    val avatarError: StateFlow<String?> = _avatarError

    init {
        fetchProfile()
    }

    fun refresh(){
        fetchProfile()
    }
    private fun fetchProfile() {
        viewModelScope.launch {
            try {
                val result = ApiClient.apiService.getMyProfile()
                _profile.value = result
                _totalBookings.value = result.total_bookings

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error fetching profile", e)
            }
        }
    }

    fun fetchGiftCards(onResult: (List<GiftCard>) -> Unit) {
        viewModelScope.launch {
            try {
                val result = ApiClient.bookingApiService.getReceivedGifts(true)
                onResult(result)
            }
            catch (e: Exception){
                Log.e("Profile view model", "Error fetching gift cards", e)
                onResult(emptyList())
            }
        }
    }
    fun uploadAvatar(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val mime = contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if(bytes == null){
                    _avatarError.value = "Could not upload the selected image"
                    return@launch
                }
                val request = bytes.toRequestBody(mime.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "avatar.jpg", request)

                val res = ApiClient.apiService.uploadAvatar(part)
                _profile.value = res
                _totalBookings.value = res.total_bookings
            }
            catch (e: Exception) {
                Log.e("ProfileViewModel", "Error fetching profile", e)
                _avatarError.value = "Failed to upload photo, please try again"
            }
        }
    }
}
