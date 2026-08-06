package com.lumeire.app

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.lumeire.app.data.model.Salon
import com.lumeire.app.data.model.Service
import com.lumeire.app.databinding.FragmentGiftingBinding
import com.lumeire.app.ui.home.HomeViewModel
import kotlinx.coroutines.launch
import okio.IOException
import retrofit2.HttpException

class GiftingFragment : Fragment() {

    private var _binding: FragmentGiftingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private var selectedOccasionIndex = 0
    private lateinit var occasionChips: List<TextView>
    private var salons: List<Salon> = emptyList()
    private var services: List<Service> = emptyList()
    private var selectedSalonIndex = 0
    private var selectedServiceIndex = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGiftingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        occasionChips = listOf(
            binding.chipOccasion1, binding.chipOccasion2, binding.chipOccasion3,
            binding.chipOccasion4, binding.chipOccasion5
        )

        occasionChips.forEachIndexed { index, chip ->
            chip.setOnClickListener {
                selectedOccasionIndex = index
                updateOccasionSelection(occasionChips)
            }
        }
        updateOccasionSelection(occasionChips)

        binding.tvPaymentReference.text = "LUM-${System.currentTimeMillis().toString().takeLast(6)}"

        lifecycleScope.launch {
            viewModel.salons.collect { salonList ->
                if (salonList.isNotEmpty()) {
                    salons = salonList
                    buildSalonChips()
                }
            }
        }

        binding.btnSendGift.setOnClickListener { handleSendGift() }
    }

    private fun buildSalonChips() {
        val container = binding.llSalonChips
        container.removeAllViews()

        salons.forEachIndexed { index, salon ->
            val chip = makeChip(salon.name, index == selectedSalonIndex)
            chip.setOnClickListener {
                selectedSalonIndex = index
                refreshSalonChips()
                fetchServicesForSalon(salon.id)
            }
            container.addView(chip)
        }

        if (salons.isNotEmpty()) fetchServicesForSalon(salons[selectedSalonIndex].id)
    }

    private fun refreshSalonChips() {
        binding.llSalonChips.forEachIndexed { index, view ->
            val chip = view as? TextView ?: return@forEachIndexed
            val selected = index == selectedSalonIndex
            chip.setBackgroundResource(if (selected) R.drawable.bg_chip_gold_filled else R.drawable.bg_chip_outlined)
            chip.setTextColor(resources.getColor(if (selected) R.color.white else R.color.text_medium, null))
        }
    }

    private fun fetchServicesForSalon(salonId: String) {
        lifecycleScope.launch {
            try {
                services = ApiClient.bookingApiService.getServices(salonId)
                selectedServiceIndex = 0
                buildServiceChips()
            } catch (e: Exception) {
                android.util.Log.e("Gifting Fragment", "Error fetching services", e)
                Toast.makeText(requireContext(), "Could not load services: ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildServiceChips() {
        val container = binding.llServiceChips
        container.removeAllViews()

        if (services.isEmpty()) {
            val empty = makeChip("No services available", false)
            container.addView(empty)
            return
        }

        services.forEachIndexed { index, service ->
            val chip = makeChip(service.name, index == selectedServiceIndex)
            chip.setOnClickListener {
                selectedServiceIndex = index
                refreshServiceChips()
                updatePreview()
            }
            container.addView(chip)
        }
        updatePreview()
    }

    private fun refreshServiceChips() {
        binding.llServiceChips.forEachIndexed { index, view ->
            val chip = view as? TextView ?: return@forEachIndexed
            val selected = index == selectedServiceIndex
            chip.setBackgroundResource(if (selected) R.drawable.bg_chip_gold_filled else R.drawable.bg_chip_outlined)
            chip.setTextColor(resources.getColor(if (selected) R.color.white else R.color.text_medium, null))
        }
    }

    private fun updatePreview() {
        if (services.isEmpty()) return
        val service = services[selectedServiceIndex]
        val salon = salons.getOrNull(selectedSalonIndex)

        binding.tvGiftPreviewTitle.text = service.name
        binding.tvGiftPreviewDesc.text = salon?.name ?: ""
        binding.tvGiftPreviewValue.text = "PKR ${service.price.toInt()}"
        binding.tvGiftPreviewDuration.text = "${service.duration_minutes} min"
        binding.tvGiftServiceName.text = service.name
        binding.tvGiftServicePrice.text = "PKR ${service.price.toInt()}"
        binding.tvGiftTotal.text = "PKR ${service.price.toInt()}"
        binding.btnSendGift.text = buildSendLabel()
    }

    private fun buildSendLabel(): String {
        if (services.isEmpty()) return getString(R.string.send_gift)
        val service = services[selectedServiceIndex]
        return "Send Gift · $${service.price}"
    }

    private fun handleSendGift() {
        val email = binding.etRecipientName.text?.toString()?.trim().orEmpty()
        val message = binding.etGiftMessage.text?.toString()?.trim().orEmpty()
        val salon = salons.getOrNull(selectedSalonIndex)
        val service = services.getOrNull(selectedServiceIndex)

        if (email.isBlank()) {
            Toast.makeText(requireContext(), "Please enter recipient's email.", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(
                requireContext(),
                "Please enter a valid email address.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (salon == null || service == null) {
            Toast.makeText(
                requireContext(),
                "Please select a salon and service first.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        binding.btnSendGift.isEnabled = false
        binding.btnSendGift.text = "Verifying..."

        lifecycleScope.launch {
            try {
                val currentUserEmail = ApiClient.authService.getMe().email
                if (email.equals(currentUserEmail, ignoreCase = true)) {
                    Toast.makeText(
                        requireContext(),
                        "You cannot send a gift to yourself",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetSend()
                    return@launch
                }
                val exists = ApiClient.bookingApiService.checkUser(email).exists
                if (!exists) {
                    Toast.makeText(
                        requireContext(),
                        "No Lumeire account found for $email.",
                        Toast.LENGTH_SHORT
                    ).show()
                    resetSend()
                    return@launch
                }
                val occasionLabel = occasionChips.getOrNull(selectedOccasionIndex)?.text?.toString()
                ApiClient.bookingApiService.sendGift(
                    GiftCardCreateRequest(
                        email,
                        salon.id,
                        service.id,
                        binding.tvGiftServicePrice.toString().toDouble() ,
                        message.ifBlank { null })
                )
                binding.btnSendGift.text = "✓ Gift Sent!"
                binding.btnSendGift.setBackgroundResource(R.drawable.bg_button_green)
                Toast.makeText(requireContext(), "Gift card sent to $email!", Toast.LENGTH_SHORT)
                    .show()
                binding.btnSendGift.postDelayed({
                    if (_binding != null) {
                        updatePreview()
                        binding.btnSendGift.isEnabled = true
                        binding.btnSendGift.setBackgroundResource(R.drawable.bg_button_gold)
                    }
                }, 2200L)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Failed tp send gift: ${toMessage(e)}",
                    Toast.LENGTH_SHORT
                ).show()
                resetSend()
            }
        }
    }


    private fun resetSend() {
        binding.btnSendGift.isEnabled = true
        binding.btnSendGift.text = buildSendLabel()
    }
    private fun makeChip(label: String, selected: Boolean): TextView {
        return TextView(requireContext()).apply {
            text = label
            setBackgroundResource(if (selected) R.drawable.bg_chip_gold_filled else R.drawable.bg_chip_outlined)
            setTextColor(resources.getColor(if (selected) R.color.white else R.color.text_medium, null))
            textSize = 14f
            setPadding(
                (16 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt()
            )
            val params = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = (10 * resources.displayMetrics.density).toInt()
            layoutParams = params
        }
    }

    private fun ViewGroup.forEachIndexed(action: (Int, View) -> Unit) {
        for (i in 0 until childCount) action(i, getChildAt(i))
    }

    private fun updateOccasionSelection(chips: List<TextView>) {
        chips.forEachIndexed { index, chip ->
            val selected = index == selectedOccasionIndex
            chip.setBackgroundResource(if (selected) R.drawable.bg_chip_gold_filled else R.drawable.bg_chip_outlined)
            chip.setTextColor(resources.getColor(if (selected) R.color.white else R.color.text_medium, null))
        }
    }
private fun toMessage(e: Exception) : String {
    var message = "Something went wrong. Please try again"
    when(e) {

        is HttpException -> {
            val code =e.code()
            when(code) {
                401 -> message = "Please login again"
                404 -> message = "Not found"
                in 500..599 -> "Server is down. Try again in a while"
                else -> message = "Something went wrong. $code"
            }
        }

        is IOException -> message = "Could not connect. Check your internet conenction."
    }
    return message
}
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
