package com.lumeire.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.lumeire.app.data.model.Salon
import com.lumeire.app.data.model.Service
import com.lumeire.app.data.model.Stylist
import com.lumeire.app.databinding.ActivitySalonBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okio.IOException
import retrofit2.HttpException

class SalonActivity: AppCompatActivity() {
    private lateinit var binding: ActivitySalonBinding
    private var salon: Salon? = null
    private var stylists: List<Stylist> = emptyList()

    companion object {
        const val EXTRA_SALON_ID = "salon_id"
        fun start(context: Context, salonId: String) {
            context.startActivity(Intent(context, SalonActivity::class.java).putExtra(EXTRA_SALON_ID, salonId))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val salonId = intent.getStringExtra(EXTRA_SALON_ID)
        if(salonId == null){
            finish()
            return
        }
        binding.btnBack.setOnClickListener {
            finish()
        }
        loadData(salonId)
    }

    private fun loadData(id: String) {
        binding.progressDetail.visibility =  View.VISIBLE
        lifecycleScope.launch {
            try {
                val salonDeferred = async { ApiClient.bookingApiService.getSalon(id) }
                val servicesDeferred = async { ApiClient.bookingApiService.getServices(id) }
                val stylistsDeferred = async { ApiClient.bookingApiService.getStylists(id) }

                salon = salonDeferred.await()
                val services = servicesDeferred.await()
                stylists = stylistsDeferred.await()
                renderSalon(salon)
                renderStylists(stylists)
                renderServices(services)
            }
            catch (e: Exception) {
                Toast.makeText(this@SalonActivity, "Failed to load salon: ${toUserMessage(e)}", Toast.LENGTH_SHORT).show()
                finish()
            }
            finally {
                binding.progressDetail.visibility = View.GONE
            }
        }
    }

    private fun renderSalon(salon: Salon?) {
        Glide.with(this).load(ApiClient.resolve(salon?.image_url)).placeholder(R.drawable.bg_button_green)
            .error(R.drawable.bg_button_green).centerCrop().into(binding.ivDetailBanner)

        binding.tvDetailName.text = salon?.name
        binding.tvDetailCategory.text = salon?.category ?: "Salon"
        binding.tvDetailRating.text = "${salon?.rating} ?: 0.0 (${salon?.review_count}) reviews"
        binding.tvDetailAddress.text = salon?.address ?: ""
        if(!(salon?.openTime.isNullOrBlank() || salon.closeTime.isNullOrBlank()))
            binding.tvDetailHours.text = "Open ${salon.openTime} - ${salon.closeTime}"
        else
            binding.tvDetailHours.text = ""

    }

    private fun renderStylists(stylists: List<Stylist>) {
        if(stylists.isEmpty()) {
            binding.layoutStylistsSection.visibility = View.GONE
            return
        }
        binding.layoutStylistsSection.visibility = View.VISIBLE
        binding.layoutStylistsContainer.removeAllViews()
        stylists.forEach {s ->
            val chip = TextView(this).apply {
                text = if (s.speciality.isNullOrBlank()) s.name else "${s.name}\n${s.speciality}"
                setTextColor(resources.getColor(R.color.text_medium, null))
                setBackgroundResource(R.drawable.bg_chip_outlined)
                setPadding(24,16,24,16)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = 10 }
            }
            binding.layoutStylistsContainer.addView(chip)
        }
    }

    private fun renderServices(services: List<Service>) {
        binding.layoutServicesContainer.removeAllViews()

        if(services.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No services available for this salon"
                gravity = Gravity.CENTER
                setPadding(0,50,0,50)
            }
            binding.layoutServicesContainer.addView(tv)
            return
        }
        services.forEach {s ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_card_white)
                setPadding(20,20,20,20)
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0,0,0,12) }
            }
            val infoCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            infoCol.addView(TextView(this).apply{
                text = s.name
                setTextColor(resources.getColor(R.color.text_dark, null))
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
            })
            infoCol.addView(TextView(this).apply{
                text = "${s.duration_minutes} min - ${s.currency} ${s.price.toInt()}"
                setTextColor(resources.getColor(R.color.text_medium, null))
                textSize = 13f
                setPadding(0,4,0,0)
            })
            val bookBtn = Button(this).apply {
                text = "Book"
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.bg_button_gold)
                setOnClickListener { openBooking(s) }
            }
            row.addView(infoCol)
            row.addView(bookBtn)
            binding.layoutServicesContainer.addView(row)
        }
    }
    private fun openBooking(service: Service) {
        val currSalon = salon ?: return
        val sheet = BookingBottomSheet.newInstance(currSalon, service, stylists)
        sheet.onBookingConfirmed = {
            setResult(RESULT_OK)
            finish()
        }
        sheet.show(supportFragmentManager, "BookingBottomSheet")
    }

    private fun toUserMessage(e: Exception): String {
        var message = ""
        when(e) {
            is HttpException -> {
                val code = e.code()
                message = when (code) {
                    401 -> "Please log in again"
                    404 -> "Not Found"
                    in 500..599 -> "Something went wrong on our end. Please try again."
                    else -> "Something went wrong (${code})"
                }
            }
            is IOException -> {
                message = "Couldn't connect. Check your internet connection."
            }
            else -> {
                message = "Something went wrong. Please try again."
            }
        }
        return message
    }
}