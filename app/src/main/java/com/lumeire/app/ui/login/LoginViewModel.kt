package com.lumeire.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumeire.app.ApiClient
import com.lumeire.app.ErrorResponse
import com.lumeire.app.GoogleAuth
import com.lumeire.app.Otp
import com.lumeire.app.ResendOtp
import com.lumeire.app.SignIn
import com.lumeire.app.SignUp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import retrofit2.HttpException


class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState
    private val _optState = MutableStateFlow<OptState>(OptState.Idle)
    val optState: StateFlow<OptState> = _optState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = ApiClient.authService.login(SignIn(email, password))
                ApiClient.saveToken(response.access_token)
                _loginState.value = LoginState.Success
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(parseError(e))
            }
        }
    }
    
    fun register(email: String, password: String, fullname: String) {
        viewModelScope.launch {
            _optState.value = OptState.Loading
            try {
                ApiClient.authService.register(SignUp(email, password, fullname))
                _optState.value = OptState.CodeSent(email)
            } catch (e: Exception) {
                _optState.value = OptState.Error(parseError(e))
            }
        }
    }

    fun verifyOtp(email: String, code: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try{
                val response = ApiClient.authService.verifyOtp(Otp(email, code))
                ApiClient.saveToken(response.access_token)
                _loginState.value = LoginState.Success
            }
            catch(e: Exception) {
                _loginState.value = LoginState.Error(parseError(e))
            }
        }
    }

    fun resendOtp(email: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                ApiClient.authService.resendOtp(ResendOtp(email))
                onDone(true, null)
            }
            catch (e: Exception) {
                onDone(false, parseError(e))
            }
        }
    }

    fun googleAuth(idToken: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = ApiClient.authService.googleAuth(GoogleAuth(idToken))
                ApiClient.saveToken(response.access_token)
                _loginState.value = LoginState.Success
            }
            catch (e: Exception) {
                _loginState.value = LoginState.Error(parseError(e))
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
    private fun parseError(e : Exception): String {
        if (e is HttpException) {
            return try {
                val body = e.response()?.errorBody()?.string()
                if (body != null)
                    Json.decodeFromString<ErrorResponse>(body).detail
                else
                    "Something went wrong"
            }
            catch (parseEx: Exception) {
                "Something went wrong"
            }
        }
        return e.message ?: "Network Error, please try again"
    }

}


sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
sealed class OptState {
    object Idle : OptState()
    object Loading : OptState()
    data class CodeSent(val email: String) : OptState()
    data class Error(val message: String) : OptState()
}
