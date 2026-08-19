package com.lumeire.app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lumeire.app.databinding.ActivityVouchersBinding
import kotlinx.coroutines.launch

class VoucherActivity: AppCompatActivity() {
    private lateinit var binding: ActivityVouchersBinding
    private lateinit var adapter: VoucherAdapter
    private var used = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVouchersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = VoucherAdapter(emptyList())
        binding.rvVouchers.layoutManager = LinearLayoutManager(this)
        binding.rvVouchers.adapter = adapter


        binding.btnBack.setOnClickListener { finish() }
        binding.tabActive.setOnClickListener {
            select(false)
        }
        binding.tabUsed.setOnClickListener {
            select(true)
        }
        loadVouchers()
    }
    private fun select(fUsed: Boolean) {
        used = fUsed
        if(fUsed){
            binding.tabActive.setTextColor(resources.getColor(R.color.text_muted, null))
            binding.tabUsed.setTextColor(resources.getColor(R.color.text_dark, null))
        }
        else{
            binding.tabActive.setTextColor(resources.getColor(R.color.text_dark, null))
            binding.tabUsed.setTextColor(resources.getColor(R.color.text_muted, null))
        }
    }
    private fun loadVouchers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
        binding.rvVouchers.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val all = ApiClient.voucherService.getMyVouchers(unusedOnly = !used)
                if(all.isEmpty()) {
                    binding.tvEmptyMessage.text = if (used) getString(R.string.no_vouchers_used) else getString(R.string.no_vouchers_un)
                    binding.tvEmptyMessage.visibility = View.VISIBLE
                }
                else{
                    adapter.submitList(all)
                    binding.rvVouchers.visibility = View.VISIBLE
                }
            }
            catch (_: Exception) {
                Toast.makeText(this@VoucherActivity, R.string.generic_load_error, Toast.LENGTH_SHORT).show()
            }
            finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}