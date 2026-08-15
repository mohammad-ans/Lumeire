package com.lumeire.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lumeire.app.databinding.ItemVoucherBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class VoucherAdapter(private var items: List<VoucherResponse>) : RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder>(){
    private val isos = listOf(
        "yyyy-MM-dd'T'HH:mm:SS:SSSSSS",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    private val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun submitList(newItems: List<VoucherResponse>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val binding = ItemVoucherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VoucherViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }
    private fun formatDate(s: String): String{
        for (i in isos) {
            try {
                val parser = SimpleDateFormat(i, Locale.getDefault())
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(s)
                if (date == null)
                    continue
                return format.format(date)
            }
            catch (_ : Exception) {

            }
        }
        return s.substringBefore("T")
    }

    private fun label(r: String?): String{
        return when(r) {
            "first_visit" -> "Welcome offer... first visit"
            null -> "Discount Voucher"
            else -> r.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
    }

    inner class VoucherViewHolder(private val binding: ItemVoucherBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(voucher: VoucherResponse) {
            binding.tvVoucherCode.text = voucher.code
            binding.tvVoucherDiscount.text = "${voucher.discount_value.toInt()}% off"
            if(voucher.discount_type != "percent")
                binding.tvVoucherDiscount.text = "${voucher.discount_value.toInt()} off"
            binding.tvVoucherReason.text = label(voucher.reason)
            binding.tvVoucherExpiry.text = voucher.expires_at?.let { "Expires ${formatDate(it)}" } ?: "No expiry"
            if(voucher.is_used) {
                binding.tvVoucherStatus.text = "USED"
            }
            else{
                binding.tvVoucherStatus.text = "ACTIVE"
            }

        }
    }
}