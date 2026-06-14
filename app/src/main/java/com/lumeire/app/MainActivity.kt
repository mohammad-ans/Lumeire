package com.lumeire.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.lumeire.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var suppressNavCallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showTab(R.id.nav_home, HomeFragment())

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

    fun openBookings() {
        showTab(R.id.nav_bookings, BookingsFragment())
    }

    fun openMaps(salonId: Int? = null) {
        showTab(R.id.nav_maps, MapsFragment.newInstance(salonId))
    }

    private fun showTab(menuId: Int, fragment: Fragment) {
        suppressNavCallback = true
        binding.bottomNavigation.selectedItemId = menuId
        suppressNavCallback = false
        loadFragment(fragment)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
