package com.lumeire.app

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lumeire.app.databinding.ActivityRewardsBinding
import kotlinx.coroutines.launch

class RewardsActivity: AppCompatActivity() {
    private lateinit var binding: ActivityRewardsBinding
    private data class Tier(val name: String, val threshold: Int)

    private val tiers = listOf(
        Tier("Bronze", 0),
        Tier("Silver", 500),
        Tier("Gold", 1000),
        Tier("Platinum", 2500)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        buildTiers(0)
        loadProfile()
    }
    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val prof = ApiClient.apiService.getMyProfile()
                binding.tvCurrentTier.text = getString(R.string.tier_member_badge, prof.loyalty_tier)
                binding.tvPointsValue.text = prof.reward_points.toString()
                binding.pbTierProgress.progress = prof.tier_progress.toInt().coerceIn(0, 100)
                binding.tvNextTierInfo.text = if(prof.next_tier != null) getString(R.string.point_to_next_tier, prof.points_next_tier ?: 0, prof.next_tier) else getString(R.string.max_tier_reached, prof.loyalty_tier)
                buildTiers(prof.reward_points)
            }
            catch (e: Exception) {
                Log.e("Rewards", "Failed to load rewards", e)
                Toast.makeText(this@RewardsActivity, R.string.generic_load_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildTiers(curr: Int) {
        binding.tierListContainer.removeAllViews()
        tiers.forEachIndexed {i, t ->
            val reached = curr >= t.threshold
            val row = TextView(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(48,40,48,40)
                if(reached){
                    text = getString(R.string.tier_row_reached, t.name, t.threshold)
                    setTextColor(resources.getColor(R.color.text_dark, null))
                    setTypeface(typeface, Typeface.BOLD)
                }
                else{
                    text = getString(R.string.tier_row_locked, t.name, t.threshold)
                    setTextColor(resources.getColor(R.color.text_muted, null))
                }
            }
            binding.tierListContainer.addView(row)
            if (i != tiers.lastIndex) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
                    setBackgroundColor(resources.getColor(R.color.cream_bg, null))
                }
                binding.tierListContainer.addView(divider)
            }
        }
    }
}