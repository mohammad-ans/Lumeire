package com.lumeire.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumeire.app.di.SupabaseModule
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                SupabaseModule.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                    // this.data = buildJsonObject { put("full_name", fullname) }
                }
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun register(email: String, password: String, fullname: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                SupabaseModule.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                     this.data = buildJsonObject { put("full_name", fullname) }
                }
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
