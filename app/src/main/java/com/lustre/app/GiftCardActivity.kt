package com.lustre.app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lustre.app.databinding.ActivityGiftCardsBinding
import kotlinx.coroutines.launch


class GiftCardActivity: AppCompatActivity() {
    private lateinit var binding: ActivityGiftCardsBinding
    private lateinit var adapter: GiftCardAdapter

    private var currentMode = GiftCardAdapter.Mode.RECEIVED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGiftCardsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        adapter = GiftCardAdapter(emptyList(), currentMode)
        binding.rvGiftCards.layoutManager = LinearLayoutManager(this)
        binding.rvGiftCards.adapter = adapter
        binding.tabReceived.setOnClickListener {
            select(GiftCardAdapter.Mode.RECEIVED)
        }
        binding.tabSent.setOnClickListener {
            select(GiftCardAdapter.Mode.SENT)
        }

        select(GiftCardAdapter.Mode.RECEIVED)
    }
    private fun select(mode: GiftCardAdapter.Mode) {
        currentMode = mode
        val received = mode == GiftCardAdapter.Mode.RECEIVED
        if(received){
            binding.tabReceived.setTextColor(resources.getColor(R.color.text_dark, null))
            binding.tabSent.setTextColor(resources.getColor(R.color.text_muted, null))
        }
        else {
            binding.tabReceived.setTextColor(resources.getColor(R.color.text_muted, null))
            binding.tabSent.setTextColor(resources.getColor(R.color.text_dark, null))
        }
        loadGifts(mode)
    }
    private fun loadGifts(mode: GiftCardAdapter.Mode) {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
        binding.rvGiftCards.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val gifts = when(mode){
                    GiftCardAdapter.Mode.RECEIVED -> ApiClient.bookingApiService.getReceivedGifts(false)
                    GiftCardAdapter.Mode.SENT -> ApiClient.bookingApiService.getSendGifts()
                }
                if(gifts.isEmpty()) {
                    binding.tvEmptyMessage.text = if(mode == GiftCardAdapter.Mode.RECEIVED) getString(R.string.no_gift_cards_received) else getString(R.string.no_gift_cards_sent)
                    binding.emptyState.visibility = View.VISIBLE
                }
                else {
                    adapter = GiftCardAdapter(gifts, mode)
                    binding.rvGiftCards.adapter = adapter
                    binding.rvGiftCards.visibility = View.VISIBLE
                }
            }
            catch (e : Exception) {
                Log.e("Gift Cards", "Failed to get gift cards", e)
                Toast.makeText(this@GiftCardActivity, R.string.generic_load_error, Toast.LENGTH_SHORT).show()
            }
            finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}