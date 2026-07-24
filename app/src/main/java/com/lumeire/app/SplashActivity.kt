package com.lumeire.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.lumeire.app.di.SupabaseModule
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch
import android.widget.Toast
import com.lumeire.app.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startAnimations()

        // Navigate to Onboarding after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndNavigate()
        }, 3000)
    }
        private fun checkAuthAndNavigate() {
            lifecycleScope.launch {
                try {
                    SupabaseModule.client.auth.loadFromStorage() // now actually loads from DataStore
                    val session = SupabaseModule.client.auth.currentSessionOrNull()
                    if (session != null) {
                        showBiometricPrompt() //  biometric gate for returning users
                    } else {
                        startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))
                        finish()
                    }
                } catch (e: Exception) {
                    startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))
                    finish()
                }
            }
        }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    finish()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login to Lumeire")
            .setSubtitle("Use your biometric credential or PIN/Pattern")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun startAnimations() {
        binding.splashContent.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.tvBrandName.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(500)
            .start()

        binding.dividerRow.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(800)
            .start()

        binding.tvTagline.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(1100)
            .start()

        binding.loadingContainer.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(1500)
            .start()

        // Simple progress bar animation
        binding.progressBarFill.post {
            val width = binding.loadingContainer.width
            binding.progressBarFill.layoutParams.width = 0
            binding.progressBarFill.animate()
                .translationX(0f)
                .scaleX(1f)
                .setDuration(2000)
                .setStartDelay(1000)
                .withStartAction {
                    val params = binding.progressBarFill.layoutParams
                    params.width = width
                    binding.progressBarFill.layoutParams = params
                    binding.progressBarFill.scaleX = 0f
                    binding.progressBarFill.pivotX = 0f
                }
                .start()
        }
    }
}

