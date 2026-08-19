package com.lumeire.app

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lumeire.app.databinding.ActivityNotificationsBinding
import kotlinx.coroutines.launch
import kotlin.io.path.Path

class NotificationsActivity: AppCompatActivity() {
    private lateinit var binding: ActivityNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.tvMarkAllRead.setOnClickListener {
            markAllRead()
        }
        loadNotifications()
    }

    private fun loadNotifications() {
        lifecycleScope.launch {
            try {
                renderNotifications(ApiClient.apiService.getNotifications())
            }
            catch (e: Exception) {
                renderMessage("Could not load notifications, check your internet connection")
            }
        }
    }

    private fun renderNotifications(notifications: List<Notification>) {
        binding.layoutNotificationsContainer.removeAllViews()
        if(notifications.isEmpty()) {
            renderMessage("You do not have any notifications yet")
            return
        }
        notifications.forEach { binding.layoutNotificationsContainer.addView(createNotification(it)) }
    }

    private fun renderMessage(text: String) {
        binding.layoutNotificationsContainer.removeAllViews()
        val tv = TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            setPadding(0,80,0,80)
        }
        binding.layoutNotificationsContainer.addView(tv)
    }

    private fun createNotification(n: Notification) : View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20,20,20,20)
            if(n.is_read)
                setBackgroundResource(R.drawable.bg_button_white)
            else
                setBackgroundResource(R.drawable.bg_chip_gold_filled)
        }
        val title = TextView(this).apply {
            text = n.title
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            if(n.is_read)
                setTextColor(resources.getColor(R.color.text_dark, null))
            else
                setTextColor(resources.getColor(R.color.white, null))
        }
        val body = TextView(this).apply {
            text = n.body
            textSize = 13f
            setPadding(0,6,0,0)

            if(n.is_read)
                setTextColor(resources.getColor(R.color.text_medium, null))
            else
                setTextColor(resources.getColor(R.color.white, null))
        }
        container.addView(title)
        container.addView(body)
        container.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0,0,0,12)
        }
        if(!n.is_read){
            container.setOnClickListener {
                lifecycleScope.launch {
                    try {
                        ApiClient.apiService.markRead(n.id)
                        loadNotifications()
                    }
                    catch (_: Exception) {

                    }
                }
            }
        }
        return container
    }

    private fun markAllRead() {
        lifecycleScope.launch {
            try {
                ApiClient.apiService.markAllRead()
                loadNotifications()
            }
            catch (_ : Exception) {

            }
        }
    }
}