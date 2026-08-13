package com.lumeire.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lumeire.app.databinding.ItemFaqBinding

data class Faq(val q: String, val ans: String)
class FaqAdapter(private val items: List<Faq>) : RecyclerView.Adapter<FaqAdapter.FaqViewHolder>() {
    private val positions = mutableSetOf<Int>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val binding = ItemFaqBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FaqViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        holder.bind(items[position], positions.contains(position))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class FaqViewHolder(private val binding: ItemFaqBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(faq: Faq, expanded: Boolean) {
            binding.tvFaqQuestion.text = faq.q
            binding.tvFaqAnswer.text = faq.ans
            binding.tvFaqAnswer.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.ivFaqChevron.rotation = if (expanded) 180f else 0f

            binding.rowFaqQuestion.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION)
                    return@setOnClickListener
                if(positions.contains(position))
                    positions.remove(position)
                else
                    positions.add(position)
                notifyItemChanged(position)
            }
        }
    }
}