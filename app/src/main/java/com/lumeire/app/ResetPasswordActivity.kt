package com.lumeire.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lumeire.app.databinding.ActivityResetPasswordBinding
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {
    companion object{
        const val EXTRA_EMAIL = "extra_email"
    }

    private lateinit var binding: ActivityResetPasswordBinding
    private val viewModel: ForgotPasswordViewModel by viewModels()
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        email = intent.getStringExtra(EXTRA_EMAIL) ?: run {
            finish()
            return
        }
        binding.tvSubtitle.text = ""
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.tvResendCode.setOnClickListener {
            viewModel.requestReset(email)
            Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
        }

        binding.btnResetPassword.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            val newPassword = binding.etNewPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            when {
                otp.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                    Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
                }
                newPassword.length < 8 -> {
                    Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    viewModel.resetPassword(email, otp, newPassword)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.resetState.collect {state ->
                when(state) {
                    is ResetPasswordState.Loading -> {
                        binding.btnResetPassword.isEnabled = false
                        binding.btnResetPassword.text = ""
                    }
                    is ResetPasswordState.Success -> {
                        Toast.makeText(this@ResetPasswordActivity, state.message, Toast.LENGTH_LONG).show()
                        val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                    is ResetPasswordState.Error -> {
                        binding.btnResetPassword.isEnabled = true
                        binding.btnResetPassword.text = ""
                    }
                    is ResetPasswordState.Idle -> {
                        binding.btnResetPassword.isEnabled = true
                        binding.btnResetPassword.text = ""
                    }
                }
            }
        }
    }
}