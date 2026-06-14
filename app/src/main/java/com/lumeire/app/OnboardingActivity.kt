package com.lumeire.app

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.lumeire.app.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private var currentStep = 0
    private lateinit var dots: List<View>

    private val onboardingSteps = listOf(
        OnboardingStep(
            R.string.onboarding_title_1,
            R.string.onboarding_subtitle_1,
            "https://images.unsplash.com/photo-1560066984-138dadb4c035?auto=format&fit=crop&w=800&q=80",
            "PREMIUM SALONS"
        ),
        OnboardingStep(
            R.string.onboarding_title_2,
            R.string.onboarding_subtitle_2,
            "https://images.unsplash.com/photo-1522337660859-02fbefca4702?auto=format&fit=crop&w=800&q=80",
            "EXPERT CARE"
        ),
        OnboardingStep(
            R.string.onboarding_title_3,
            R.string.onboarding_subtitle_3,
            "https://images.unsplash.com/photo-1512290923902-8a9f81dc236c?auto=format&fit=crop&w=800&q=80",
            "GIFT LUXURY"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSlides()
        setupDots()
        updateUI()

        binding.btnNext.setOnClickListener {
            if (currentStep < onboardingSteps.size - 1) {
                currentStep++
                updateUI()
            } else {
                navigateToLogin()
            }
        }

        binding.tvSkip.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun setupSlides() {
        onboardingSteps.forEach { step ->
            binding.viewFlipper.addView(createSlideView(step))
        }
        // Smooth transitions between slides
        binding.viewFlipper.setInAnimation(this, android.R.anim.fade_in)
        binding.viewFlipper.setOutAnimation(this, android.R.anim.fade_out)
    }

    private fun setupDots() {
        dots = onboardingSteps.mapIndexed { index, _ ->
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    if (index == 0) 24.dp else 8.dp,
                    8.dp
                ).apply {
                    if (index > 0) marginStart = 8.dp
                }
                background = getDrawable(if (index == 0) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            }.also(binding.dotsContainer::addView)
        }
    }

    private fun createSlideView(step: OnboardingStep): View {
        val container = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
        }

        val badge = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            background = getDrawable(R.drawable.bg_card_white)
            setPadding(12.dp, 12.dp, 12.dp, 12.dp)
            elevation = 6.dp.toFloat()
        }

        val image = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(280.dp, 210.dp)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        Glide.with(this)
            .load(step.imageUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .transform(CenterCrop(), RoundedCorners(20.dp))
            .into(image)

        val label = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 14.dp
                bottomMargin = 4.dp
            }
            text = step.label
            setTextColor(getColor(R.color.gold_dark))
            textSize = 12f
            letterSpacing = 0.2f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        badge.addView(image)
        badge.addView(label)
        container.addView(badge)
        return container
    }

    private fun updateUI() {
        val step = onboardingSteps[currentStep]
        binding.tvTitle.text = getString(step.titleRes)
        binding.tvSubtitle.text = getString(step.subtitleRes)
        binding.viewFlipper.displayedChild = currentStep
        updateDots()

        if (currentStep == onboardingSteps.size - 1) {
            binding.btnNext.text = getString(R.string.btn_get_started)
        } else {
            binding.btnNext.text = getString(R.string.btn_next)
        }
    }

    private fun updateDots() {
        dots.forEachIndexed { index, dot ->
            dot.layoutParams = (dot.layoutParams as LinearLayout.LayoutParams).apply {
                width = if (index == currentStep) 24.dp else 8.dp
            }
            dot.background = getDrawable(
                if (index == currentStep) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
            )
            dot.requestLayout()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    data class OnboardingStep(val titleRes: Int, val subtitleRes: Int, val imageUrl: String, val label: String)
}
