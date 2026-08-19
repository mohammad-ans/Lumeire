package com.lustre.app

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lustre.app.databinding.ActivitySettingsBinding
import kotlinx.coroutines.launch

class SettingsActivity: AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = getSharedPreferences(PushPreferences.PREFS_NAME, Context.MODE_PRIVATE)
        binding.btnBack.setOnClickListener { finish() }

        binding.switchPushNotifications.isChecked = prefs.getBoolean(PushPreferences.KEY_PUSH_NOTIFICATIONS, true)
        binding.switchBookingReminders.isChecked = prefs.getBoolean(PushPreferences.KEY_BOOKING_REMINDERS, true)
        binding.switchDarkMode.isChecked = prefs.getBoolean(PushPreferences.KEY_DARK_MODE, false)
        binding.tvAppVersion.text = getVersion()
        binding.switchPushNotifications.setOnCheckedChangeListener {_, isChecked ->
            prefs.edit().putBoolean(PushPreferences.KEY_PUSH_NOTIFICATIONS, isChecked).apply()
            syncPushPreference(isChecked)
        }
        binding.switchBookingReminders.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PushPreferences.KEY_BOOKING_REMINDERS, isChecked).apply()
        }
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PushPreferences.KEY_DARK_MODE, isChecked).apply()
            Toast.makeText(this, getString(R.string.dark_mode_restart_note), Toast.LENGTH_SHORT).show()
        }
    }
    fun syncPushPreference(enabled: Boolean) {
        if(!enabled) {
            lifecycleScope.launch {
                try {
                    ApiClient.apiService.updateProfile(ProfileUpdateRequest(fcm_token = ""))
                }
                catch (e: Exception) {
                    Log.e("Settings", "Failed to clear push token", e)
                }
            }
            return
        }
    }
    private fun getVersion(): String {
        return try {
            val info = packageManager.getPackageInfo(packageName, 0)
            info.versionName ?: "1.0.0"
        }
        catch(e : PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }
}