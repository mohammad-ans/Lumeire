package com.lumeire.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lumeire.app.databinding.ItemTicketBinding
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.graphics.toColorInt
import java.util.TimeZone

class TicketsAdapter(private var items: List<SupportTicket>): RecyclerView.Adapter<TicketsAdapter.TicketViewHolder>() {
    private val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    fun submitList(newItems: List<SupportTicket>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        return holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }
    private fun formatDate(s: String): String{
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(s.take(19)) ?: return s.substringBefore("T")
            format.format(date)
        }
        catch (_: Exception) {
            s.substringBefore("T")
        }
    }
    inner class TicketViewHolder(private val binding: ItemTicketBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ticket: SupportTicket) {
            binding.tvTicketSbj.text = ticket.sbj
            binding.tvTicketMessage.text = ticket.msg
            binding.tvTicketDate.text = formatDate(ticket.created_at)
            binding.tvTicketStatus.text = ticket.status.replace("_", " ").uppercase()
            binding.tvTicketStatus.setTextColor(
                when(ticket.status) {
                    "resolved" -> "#2E7D32".toColorInt()
                    "in_progress" -> "#B8860B".toColorInt()
                    else -> "#9E9E9E".toColorInt()
                }
            )
        }
    }
}