package com.lumeire.app

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lumeire.app.databinding.ActivityOtpBinding
import com.lumeire.app.ui.login.LoginState
import com.lumeire.app.ui.login.LoginViewModel
import kotlinx.coroutines.launch
import androidx.activity.viewModels

class OtpActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }
    private lateinit var binding: ActivityOtpBinding
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        email = intent.getStringExtra(EXTRA_EMAIL) ?: run {
            finish()
            return
        }
        binding.tvEmailLabel.text = getString(R.string.otp_sent_to, email)
        binding.btnVerify.setOnClickListener {
            val code = binding.etOtpCode.text.toString().trim()
            if(code.length < 6){
                Toast.makeText(this, "Enter the code from your email", Toast.LENGTH_LONG).show()
            }
            else{
                viewModel.verifyOtp(email, code)
            }
        }
        binding.btnResend.setOnClickListener {
            binding.btnResend.isEnabled = false
            viewModel.resendOtp(email) { success, error ->
                binding.btnResend.isEnabled = true
                val msg = if (success) "Code resent" else (error ?: "Could not resend code")
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()


            }
        }

        lifecycleScope.launch {
            viewModel.loginState.collect {state ->
                when(state){
                    is LoginState.Loading -> {
                        binding.btnVerify.isEnabled = false
                        binding.btnVerify.text = "Verifying..."
                    }
                    is LoginState.Success -> {
                        startActivity(Intent(this@OtpActivity, MainActivity::class.java))
                        finish()
                    }
                    is LoginState.Error -> {
                        binding.btnVerify.isEnabled = true
                        binding.btnVerify.text = getString(R.string.verify)
                        Toast.makeText(this@OtpActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is LoginState.Idle -> {
                        binding.btnVerify.isEnabled = true
                        binding.btnVerify.text = getString(R.string.verify)
                    }
                }

            }
        }
    }
}