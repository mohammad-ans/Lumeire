package com.lustre.app.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lustre.app.ApiClient
import com.lustre.app.ErrorResponse
import com.lustre.app.GoogleAuth
import com.lustre.app.Otp
import com.lustre.app.ResendOtp
import com.lustre.app.SignIn
import com.lustre.app.SignUp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import retrofit2.HttpException


class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState
    private val _otpState = MutableStateFlow<OtpState>(OtpState.Idle)
    val otpState: StateFlow<OtpState> = _otpState

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

    fun register(email: String, password: String, fullname: String, referralCode: String?) {
        viewModelScope.launch {
            _otpState.value = OtpState.Loading
            try {
                ApiClient.authService.register(SignUp(email, password, fullname, referralCode))
                _otpState.value = OtpState.CodeSent(email)
            } catch (e: Exception) {
                _otpState.value = OtpState.Error(parseError(e))
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

    fun googleAuth(idToken: String, referralCode: String? = null) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = ApiClient.authService.googleAuth(GoogleAuth(idToken, referralCode))
                ApiClient.saveToken(response.access_token)
                _loginState.value = LoginState.Success
            }
            catch (e: Exception) {
                Log.e("Log", "$e")
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
sealed class OtpState {
    object Idle : OtpState()
    object Loading : OtpState()
    data class CodeSent(val email: String) : OtpState()
    data class Error(val message: String) : OtpState()
}
