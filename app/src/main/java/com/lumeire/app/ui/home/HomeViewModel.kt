package com.lumeire.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumeire.app.data.model.Profile
import com.lumeire.app.data.model.Salon
import com.lumeire.app.di.SupabaseModule
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _salons = MutableStateFlow<List<Salon>>(emptyList())
    val salons: StateFlow<List<Salon>> = _salons

    private val _searchQuery = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow<String?>(null)

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

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCategoryFilter(category: String?) {
        _categoryFilter.value = category
    }

    private fun fetchSalons() {
        viewModelScope.launch {
            try {
                val result = SupabaseModule.client.postgrest["salons"].select(columns = Columns.ALL).decodeList<Salon>()
                _salons.value = result
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("HomeViewModel", "Error fetching salons", e)
            }
        }
    }
    // In HomeViewModel.kt
    fun checkUserExistsByEmail(email: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = SupabaseModule.client.postgrest["profiles"]
                    .select(columns = Columns.ALL) {
                        filter { eq("phone", email) }
                    }
                    .decodeList<Profile>()
                onResult(result.isNotEmpty())
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun getCurrentUserEmail(): String? {
        return SupabaseModule.client.auth.currentUserOrNull()?.email
    }
}
