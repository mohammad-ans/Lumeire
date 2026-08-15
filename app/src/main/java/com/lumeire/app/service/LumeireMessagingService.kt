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
import com.lumeire.app.ApiClient
import com.lumeire.app.MainActivity
import com.lumeire.app.ProfileUpdateRequest
import com.lumeire.app.PushPreferences
import com.lumeire.app.R
import com.lumeire.app.SettingsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class LumeireMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationCounter = AtomicInteger(0)

    companion object{
        private const val CHANNEL_ID = "lumeire_notifications"
        const val EXTRA_OPEN_NOTIFICATIONS = "open_notifications"
        const val EXTRA_RELATED_BOOKING_ID = "related_booking_id"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if(PushPreferences.getPush(applicationContext))
            updateTokenOnServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title =remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "Lumiere"
        val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: ""
        val id = remoteMessage.data["related_booking_id"]

        showNotification(title, body, id)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun updateTokenOnServer(token: String) {
        serviceScope.launch {
            try {
                if(!ApiClient.isLoggedIn())
                    return@launch
                ApiClient.apiService.updateProfile(ProfileUpdateRequest(fcm_token = token))
            }
            catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lumiere Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun showNotification(title: String, body: String, id: String?) {
        val notificationId = notificationCounter.incrementAndGet()

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
            if(id != null)
                putExtra(EXTRA_RELATED_BOOKING_ID, id)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}