package com.lustre.app

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

object PushPreferences {
    const val PREFS_NAME = "lustre_settings"
    const val KEY_PUSH_NOTIFICATIONS = "push_notifications_enabled"
    const val KEY_BOOKING_REMINDERS = "booking_reminders_enabled"
    const val KEY_DARK_MODE = "dark_mode_enabled"

    fun getPush(context: Context) : Boolean{
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PUSH_NOTIFICATIONS, true)
    }

    fun setPushed(context: Context, enabled: Boolean, scope: LifecycleCoroutineScope) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_PUSH_NOTIFICATIONS, enabled).apply()

        if(!enabled) {
            scope.launch {
                try {
                    ApiClient.apiService.updateProfile(ProfileUpdateRequest(fcm_token = ""))
                }
                catch (e: Exception) {
                    Log.e("Push Preferences", "Failed to clear fcm token", e)
                }
            }
            return
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener {task ->
            if(!task.isSuccessful) {
                Log.e("Settings", "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result ?: return@addOnCompleteListener
            scope.launch {
                try {
                    ApiClient.apiService.updateProfile(ProfileUpdateRequest(fcm_token = token))
                }
                catch (e : Exception) {
                    Log.e("Settings", "Failed to sync push notification", e)
                }
            }
        }
    }
}