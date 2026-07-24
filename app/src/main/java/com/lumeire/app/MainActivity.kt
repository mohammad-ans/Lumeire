package com.lumeire.app

import android.os.Bundle
import kotlinx.coroutines.withContext
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.lumeire.app.di.SupabaseModule
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.lumeire.app.service.FcmTokenUpdate
import com.lumeire.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var suppressNavCallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        askNotificationPermission()

        checkSessionAndRoute()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (suppressNavCallback) {
                return@setOnItemSelectedListener true
            }
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_bookings -> loadFragment(BookingsFragment())
                R.id.nav_maps -> loadFragment(MapsFragment.newInstance())
                R.id.nav_gifting -> loadFragment(GiftingFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }
    }

    private fun checkSessionAndRoute() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SupabaseModule.client.auth.loadFromStorage()
                val session = SupabaseModule.client.auth.currentSessionOrNull()

                withContext(Dispatchers.Main) {
                    if (session != null) {
                        // Authenticated — show the app
                        binding.bottomNavigation.visibility = View.VISIBLE
                        showTab(R.id.nav_home, HomeFragment())
                        fetchAndSaveFCMToken()
                    } else {
                        // No session — go to login
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish() // prevent back-navigating to a blank MainActivity
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }


    fun openBookings() {
        showTab(R.id.nav_bookings, BookingsFragment())
    }

    fun openMaps(salonId: String? = null) {
        showTab(R.id.nav_maps, MapsFragment.newInstance(salonId))
    }

    private fun showTab(menuId: Int, fragment: Fragment) {
        suppressNavCallback = true
        binding.bottomNavigation.selectedItemId = menuId
        suppressNavCallback = false
        loadFragment(fragment)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Log.w("MainActivity", "Notification permission denied")
        }
    }

    private fun fetchAndSaveFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
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
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

