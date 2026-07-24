package com.lumeire.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lumeire.app.MainActivity
import com.lumeire.app.R
import com.lumeire.app.di.SupabaseModule
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class FcmTokenUpdate(val fcm_token: String)

class LumeireMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Whenever the token is refreshed, update it in Supabase
        updateTokenInSupabase(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle the incoming message
        remoteMessage.notification?.let {
            showNotification(it.title ?: "Lumiere", it.body ?: "")
        }
    }

    private fun updateTokenInSupabase(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val session = SupabaseModule.client.auth.currentSessionOrNull()
                session?.user?.id?.let { userId ->
                    SupabaseModule.client.postgrest["profiles"]
                        .update(FcmTokenUpdate(token)) {
                            filter {
                                eq("id", userId)
                            }
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "lumeire_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lumiere Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}