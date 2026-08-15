package com.lumeire.app

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FcmTokenSync {
    fun sync(context: Context, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                if(!ApiClient.isLoggedIn())
                    return@launch
                val prefs = context.getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
                val enabled = prefs.getBoolean(SettingsActivity.KEY_PUSH_NOTIFICATIONS, true)
                if(!enabled)
                    return@launch
                val token = Tasks.await(FirebaseMessaging.getInstance().token)
                ApiClient.apiService.updateProfile(ProfileUpdateRequest(fcm_token = token))
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}