package com.lumeire.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumeire.app.ApiClient
import com.lumeire.app.data.model.Profile
import com.lumeire.app.data.model.Salon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _salons = MutableStateFlow<List<Salon>>(emptyList())
    val salons: StateFlow<List<Salon>> = _salons

    private val _searchQuery = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    val filteredSalons: StateFlow<List<Salon>> = combine(_salons, _searchQuery, _categoryFilter) { salonsList, query, category ->
        salonsList.filter { salon ->
            val matchesQuery = query.isBlank() || salon.name.contains(query, ignoreCase = true) || (salon.address?.contains(query, ignoreCase = true) == true)
            val matchesCategory = category == null || salon.name.contains(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        fetchSalons()
    }

    fun refresh(){
        fetchSalons()
    }
    fun clearError() {
        _error.value = null
    }
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCategoryFilter(category: String?) {
        _categoryFilter.value = category
    }

    private fun fetchSalons() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = ApiClient.bookingApiService.getSalons()
                _salons.value = result
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error fetching salons", e)
            }
            finally {
                _isLoading.value = false
            }
        }
    }
    fun checkUserExists(email: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = ApiClient.bookingApiService.checkUser(email)
                onResult(result.exists)
            }
            catch (e: Exception) {
                onResult(false)
            }
        }
    }
    fun fetchUserEmail(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val me = ApiClient.authService.getMe()
                onResult(me.email)
            }
            catch (e: Exception) {
                onResult(null)
            }
        }
    }
}
