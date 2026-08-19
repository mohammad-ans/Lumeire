package com.lustre.app

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.lustre.app.data.model.Salon
import com.lustre.app.data.model.Service
import com.lustre.app.databinding.FragmentGiftingBinding
import com.lustre.app.ui.home.HomeViewModel
import kotlinx.coroutines.launch
import okio.IOException
import retrofit2.HttpException

class GiftingFragment : Fragment() {

    private var _binding: FragmentGiftingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private var selectedOccasion = 0
    private lateinit var occasionChips: List<TextView>
    private var salons: List<Salon> = emptyList()
    private var services: List<Service> = emptyList()
    private var selectedSalon = 0
    private var selectedService = 0

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
                selectedOccasion = index
                updateOccasionSelection(occasionChips)
            }
        }
        updateOccasionSelection(occasionChips)

        binding.tvPaymentReference.text = "LUM-${System.currentTimeMillis().toString().takeLast(6)}"

        binding.customAmount.doAfterTextChanged {
            if(getCustomAmount() != null){
                selectedService = -1
                refreshServiceChips()
            }
            updatePreview()
        }
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

    private fun currentSalon() : Salon? {
        return salons.getOrNull(selectedSalon)
    }
    private fun getCustomAmount(): Double?{
        val raw = binding.customAmount.text?.toString()?.trim().orEmpty()
        if(raw.isBlank())
            return null
        val value = raw.toDoubleOrNull() ?: return null
        return if (value > 0) value else null
    }
    private fun buildSalonChips() {
        val container = binding.llSalonChips
        container.removeAllViews()

        salons.forEachIndexed { index, salon ->
            val chip = makeChip(salon.name, index == selectedSalon)
            chip.setOnClickListener {
                selectedSalon = index
                refreshSalonChips()
                fetchServicesForSalon(salon.id)
            }
            container.addView(chip)
        }

        if (salons.isNotEmpty())
            fetchServicesForSalon(salons[selectedSalon].id)
    }

    private fun refreshSalonChips() {
        binding.llSalonChips.forEachIndexed { index, view ->
            val chip = view as? TextView ?: return@forEachIndexed
            val selected = index == selectedSalon
            chip.setBackgroundResource(if (selected) R.drawable.bg_chip_gold_filled else R.drawable.bg_chip_outlined)
            chip.setTextColor(resources.getColor(if (selected) R.color.white else R.color.text_medium, null))
        }
    }

    private fun fetchServicesForSalon(salonId: String) {
        lifecycleScope.launch {
            try {
                services = ApiClient.bookingApiService.getServices(salonId)
                selectedService = 0
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
            val chip = makeChip(service.name, index == selectedService)
            chip.setOnClickListener {
                selectedService = index
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
            val selected = index == selectedService
            chip.setBackgroundResource(if (selected) R.drawable.bg_chip_gold_filled else R.drawable.bg_chip_outlined)
            chip.setTextColor(resources.getColor(if (selected) R.color.white else R.color.text_medium, null))
        }
    }

    private fun updatePreview() {
        val salon = currentSalon()
        val currency = salon?.currency ?: "USD"
        val customAmount = getCustomAmount()
        if(customAmount != null){
            binding.tvGiftPreviewTitle.text = "Custom Gift Amount"
            binding.tvGiftPreviewDesc.text = salon?.name ?: ""
            binding.tvGiftPreviewValue.text = "$currency ${customAmount.toInt()}"
            binding.tvGiftPreviewDuration.text = ""
            binding.tvGiftServiceName.text = "Custom Amount"
            binding.tvGiftServicePrice.text = "$currency ${customAmount.toInt()}"
            binding.tvGiftTotal.text = "$currency ${customAmount.toInt()}"
            binding.btnSendGift.text = buildSendLabel()
            return
        }

        val service = services.getOrNull(selectedService) ?: return
        binding.tvGiftPreviewTitle.text = service.name
        binding.tvGiftPreviewDesc.text = salon?.name ?: ""
        binding.tvGiftPreviewValue.text = "$currency ${service.price.toInt()}"
        binding.tvGiftPreviewDuration.text = "${service.duration_minutes} min"
        binding.tvGiftServiceName.text = service.name
        binding.tvGiftServicePrice.text = "$currency ${service.price.toInt()}"
        binding.tvGiftTotal.text = "$currency ${service.price.toInt()}"
        binding.btnSendGift.text = buildSendLabel()
    }

    private fun buildSendLabel(): String {
        val currency = currentSalon()?.currency ?: "USD"
        val amount = getCustomAmount()
        if(amount != null)
            return "Send Gift · $currency ${amount.toInt()}"
        if (services.isEmpty())
            return getString(R.string.send_gift)

        val service = services[selectedService]
        return "Send Gift · $currency ${service.price}"
    }

    private fun handleSendGift() {
        val email = binding.etRecipientName.text?.toString()?.trim().orEmpty()
        val message = binding.etGiftMessage.text?.toString()?.trim().orEmpty()
        val salon = currentSalon()
        val amount = getCustomAmount()
        val service = if(amount == null) services.getOrNull(selectedService) else null


        if (email.isBlank()) {
            Toast.makeText(requireContext(), "Please enter recipient's email.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(),"Please enter a valid email address.",Toast.LENGTH_SHORT).show()
            return
        }

        if (salon == null) {
            Toast.makeText(requireContext(),"Please select a salon first",Toast.LENGTH_SHORT).show()
            return
        }
        if(amount == null && service == null){
            Toast.makeText(requireContext(),"Please select a service or enter a custom amount.",Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSendGift.isEnabled = false
        binding.btnSendGift.text = "Verifying..."

        lifecycleScope.launch {
            try {
                val currentUserEmail = ApiClient.authService.getMe().email
                if (email.equals(currentUserEmail, ignoreCase = true)) {
                    Toast.makeText(requireContext(),"You cannot send a gift to yourself",Toast.LENGTH_SHORT).show()
                    resetSend()
                    return@launch
                }
                val exists = ApiClient.bookingApiService.checkUser(email).exists
                if (!exists) {
                    Toast.makeText(requireContext(),"No Lustre account found for $email.",Toast.LENGTH_SHORT).show()
                    resetSend()
                    return@launch
                }
                val occasionLabel = occasionChips.getOrNull(selectedOccasion)?.text?.toString()

                ApiClient.bookingApiService.sendGift(
                    GiftCardCreateRequest(
                        receiver_email =  email,
                        salon_id = salon.id,
                        service_id =  service?.id,
                        amount = amount,
                        occasion = occasionLabel?: message.ifBlank { null },
                        message = message.ifBlank { null })
                )
                binding.btnSendGift.text = "✓ Gift Sent!"
                binding.btnSendGift.setBackgroundResource(R.drawable.bg_button_green)
                Toast.makeText(requireContext(), "Gift card sent to $email!", Toast.LENGTH_SHORT)
                    .show()
                binding.btnSendGift.postDelayed({
                    if (_binding != null) {
                        binding.etRecipientName.setText("")
                        binding.etGiftMessage.setText("")
                        binding.customAmount.setText("")
                        updatePreview()
                        binding.btnSendGift.isEnabled = true
                        binding.btnSendGift.setBackgroundResource(R.drawable.bg_button_gold)
                    }
                }, 2200L)
            } catch (e: Exception) {
                Toast.makeText(requireContext(),"Failed to send gift: ${toMessage(e)}",Toast.LENGTH_SHORT).show()
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
        for (i in 0 until childCount)
            action(i, getChildAt(i))
    }

    private fun updateOccasionSelection(chips: List<TextView>) {
        chips.forEachIndexed { index, chip ->
            val selected = index == selectedOccasion
            chip.setBackgroundResource(if (selected) R.drawable.bg_chip_gold_filled else R.drawable.bg_chip_outlined)
            chip.setTextColor(resources.getColor(if (selected) R.color.white else R.color.text_medium, null))
        }
    }
private fun toMessage(e: Exception) : String {
    var message = "Something went wrong. Please try again"
    when(e) {

        is HttpException -> {
            val code = e.code()
            when(code) {
                401 -> message = "Please login again"
                404 -> message = "Not found"
                in 500..599 -> message = "Server is down. Try again in a while"
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
