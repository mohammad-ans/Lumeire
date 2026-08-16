package com.lumeire.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.lumeire.app.databinding.ActivityPaymentBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class PaymentActivity: AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private var selectedImgUri: Uri? = null

    companion object{
        const val EXTRA_BOOKING_ID = "booking_id"
        const val EXTRA_SALON_ID = "salon_id"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_CURRENCY = "currency"
    }

    private val pickImgLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {uri: Uri? ->
        if(uri == null)
            return@registerForActivityResult
        selectedImgUri = uri
        binding.ivProofPreview.visibility = View.VISIBLE
        Glide.with(this).load(uri).centerCrop().into(binding.ivProofPreview)
        binding.btnSubmitImg.isEnabled = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val bookingId = intent.getStringExtra(EXTRA_BOOKING_ID)
        val salonId = intent.getStringExtra(EXTRA_SALON_ID)
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val currency = intent.getStringExtra(EXTRA_CURRENCY) ?: "USD"
        if(bookingId == null || salonId == null) {
            Toast.makeText(this, "Something went wrong opening this booking", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        binding.tvAmountDue.text = "$currency ${amount.toInt()}"
        binding.btnPickScreenshot.setOnClickListener {
            pickImgLauncher.launch("image/*")
        }
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnCopyNumber.setOnClickListener {
            copy()
        }
        binding.btnSubmitImg.setOnClickListener {
            submitImg(bookingId)
        }
        paymentDetails(salonId)
    }

    private fun paymentDetails(id: String) {
        lifecycleScope.launch {
            try {
                val salon = ApiClient.bookingApiService.getSalon(id)
                if (salon.payment_method_name.isNullOrBlank() || salon.payment_account_number.isNullOrBlank()) {
                    binding.tvPaymentMethodName.text = "Payment details not found"
                    binding.tvPaymentAcc.text = "Contact salon ${salon.phone}"
                    binding.btnCopyNumber.visibility = View.GONE
                    return@launch
                }
                binding.tvPaymentMethodName.text = salon.payment_method_name
                binding.tvPaymentAcc.text = salon.payment_account_number
            }
            catch (_: Exception) {
                binding.tvPaymentMethodName.text = "Could not load payment details"
                binding.tvPaymentAcc.text = "Check your network and try again"
                binding.btnCopyNumber.visibility = View.GONE
            }
        }
    }
    private fun copy() {
        val number = binding.tvPaymentAcc.text.toString()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Payment Acc Number",number))
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }
    private fun submitImg(id: String) {
        val uri = selectedImgUri
        if(uri == null) {
            Toast.makeText(this, "Choose an image first", Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnSubmitImg.isEnabled = false
        binding.progressSubmit.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val mime = contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    Toast.makeText(this@PaymentActivity, "Could not read the selected image", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val reqBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "payment_proof.jpg", reqBody)
                ApiClient.bookingApiService.uploadPayment(id, part)
                Toast.makeText(this@PaymentActivity, "Payment proof submitted to the salon", Toast.LENGTH_SHORT).show()
                finish()
            }
            catch (_: Exception) {
                Toast.makeText(this@PaymentActivity, "Failed to submit image, please try again", Toast.LENGTH_SHORT).show()
                binding.btnSubmitImg.isEnabled = true
            }
            finally {
                binding.progressSubmit.visibility = View.GONE
            }
        }
    }
}