package com.lumeire.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class ForgotPasswordViewModel: ViewModel() {
    private val _requestState = MutableStateFlow<RequestResetState>(RequestResetState.Idle)
    val requestState: StateFlow<RequestResetState> = _requestState
    private val _resetState = MutableStateFlow<ResetPasswordState>(ResetPasswordState.Idle)
    val resetState: StateFlow<ResetPasswordState> = _resetState

    fun requestReset(email: String) {
        viewModelScope.launch {
            _requestState.value = RequestResetState.Loading
            try{
                val response = ApiClient.authService.forgotPassword(ForgotPassword(email))
                _requestState.value = RequestResetState.CodeSent(email, response.message)
            }
            catch(e : Exception) {
                _requestState.value = RequestResetState.Error(parseError(e))
            }
        }
    }

    fun resetRequestState() {
        _requestState.value = RequestResetState.Idle
    }
    fun resetResetState() {
        _resetState.value = ResetPasswordState.Idle
    }

    fun resetPassword(email: String, otp: String, newPsd: String) {
        viewModelScope.launch {
            _resetState.value = ResetPasswordState.Loading
            try{
                val response = ApiClient.authService.resetPassword(ResetPassword(email, otp, newPsd))
                _resetState.value = ResetPasswordState.Success("Password resetted")
            }
            catch (e: Exception) {
                _resetState.value = ResetPasswordState.Error(parseError(e))
            }
        }
    }
    private fun parseError(e: Exception): String{
        if(e is HttpException) {
            return try{
                val body = e.response()?.errorBody()?.string()
                if (body != null)
                    Json.decodeFromString<ErrorResponse>(body).detail
                else
                    "Something went wrong"
            }
            catch (ex: Exception){
                "Something went wrong"
            }

        }
        return e.message ?: "Network Error, please try again"
    }
}


sealed class RequestResetState {
    object Idle: RequestResetState()
    object Loading: RequestResetState()
    data class CodeSent(val email: String, val message: String): RequestResetState()
    data class Error(val message: String): RequestResetState()
}

sealed class ResetPasswordState {
    object Idle: ResetPasswordState()
    object Loading: ResetPasswordState()
    data class Success(val message: String): ResetPasswordState()
    data class Error(val message: String): ResetPasswordState()
}