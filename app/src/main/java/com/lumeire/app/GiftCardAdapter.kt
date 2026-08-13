package com.lumeire.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lumeire.app.databinding.ItemGiftCardBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class GiftCardAdapter(
    private var items: List<GiftCard>,
    private val mode: Mode
) : RecyclerView.Adapter<GiftCardAdapter.GiftCardViewHolder>() {
    enum class Mode {RECEIVED, SENT}
    private val isoFormatCandidates = listOf(
        "yyyy-MM-dd'T'HH:mm:ss:SSSSSS",
        "yyyy-MM0dd'T'HH:mm:ss"
    )
    private val displayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun submitList(newItems: List<GiftCard>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GiftCardViewHolder {
        val binding = ItemGiftCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GiftCardViewHolder(binding)
    }
    override fun onBindViewHolder(holder: GiftCardViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }
    private fun formatDate(s : String) : String {
        for (pattern in isoFormatCandidates) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.getDefault())
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(s) ?: continue
                return displayFormat.format(date)
            }
            catch (_ : Exception) {}
        }
        return s.substringBefore("T")
    }
    inner class GiftCardViewHolder(private val binding: ItemGiftCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(gift: GiftCard) {
            binding.tvGiftSalon.text = gift.salon_name ?: "Gift Card"
            binding.tvGiftAmount.text = "${gift.currency} ${gift.amount.toInt()}"
            binding.tvGiftDate.text = formatDate(gift.created_at)
            binding.tvGiftParty.text = when(mode) {
                Mode.RECEIVED -> "From ${gift.sender_name ?: "a friend"}"
                Mode.SENT -> "To ${gift.receiver_name ?: "recipient"}"
            }
            if (!gift.message.isNullOrBlank()) {
                binding.tvGiftMessage.visibility = android.view.View.VISIBLE
                binding.tvGiftMessage.text = gift.message
            }
            else
                binding.tvGiftMessage.visibility = android.view.View.GONE
            if(gift.is_used) {
                binding.tvGiftStatus.text = "REDEEMED"
                binding.tvGiftStatus.setTextColor(Color.parseColor("#9E9E9E"))
            }
            else{
                binding.tvGiftStatus.text = "ACTIVE"
                binding.tvGiftStatus.setTextColor(Color.parseColor("#2E7D32"))
            }
        }
    }
}