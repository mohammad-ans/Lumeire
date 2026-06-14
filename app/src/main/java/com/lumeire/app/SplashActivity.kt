package com.lumeire.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
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
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }, 3000)
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
