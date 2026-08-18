package com.lustre.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lustre.app.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordActivity: AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.tvBackToLogin.setOnClickListener {
            finish()
        }
        binding.btnSendCode.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if(email.isNotEmpty())
                viewModel.requestReset(email)
            else
                Toast.makeText(this, R.string.login_missing_fields, Toast.LENGTH_SHORT).show()
        }
        lifecycleScope.launch {
            viewModel.requestState.collect { state->
                when(state){
                    is RequestResetState.Loading -> {
                        binding.btnSendCode.isEnabled = false
                        binding.btnSendCode.text = getString(R.string.loading)
                    }
                    is RequestResetState.CodeSent -> {
                        binding.btnSendCode.isEnabled = true
                        binding.btnSendCode.text = getString(R.string.send_code)
                        Toast.makeText(this@ForgotPasswordActivity, state.message, Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java)
                        intent.putExtra(ResetPasswordActivity.EXTRA_EMAIL , state.email)
                        startActivity(intent)
                        viewModel.resetRequestState()
                    }
                    is RequestResetState.Error -> {
                        binding.btnSendCode.isEnabled = true
                        binding.btnSendCode.text = getString(R.string.send_code)
                        Toast.makeText(this@ForgotPasswordActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is RequestResetState.Idle -> {
                        binding.btnSendCode.isEnabled = true
                        binding.btnSendCode.text = getString(R.string.send_code)
                    }
                }

            }
        }
    }
}