package com.lumeire.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lumeire.app.databinding.FragmentGiftingBinding

class GiftingFragment : Fragment() {

    private var _binding: FragmentGiftingBinding? = null
    private val binding get() = _binding!!
    private var selectedGiftIndex = 0
    private var selectedOccasionIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGiftingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val serviceChips = listOf(
            binding.btnGiftService1,
            binding.btnGiftService2,
            binding.btnGiftService3,
            binding.btnGiftService4
        )
        val occasionChips = listOf(
            binding.chipOccasion1,
            binding.chipOccasion2,
            binding.chipOccasion3,
            binding.chipOccasion4,
            binding.chipOccasion5
        )

        serviceChips.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                selectedGiftIndex = index
                updateGiftSelection(serviceChips)
            }
        }

        occasionChips.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                selectedOccasionIndex = index
                updateOccasionSelection(occasionChips)
            }
        }

        binding.btnSendGift.setOnClickListener {
            val recipient = binding.etRecipientName.text?.toString()?.trim().orEmpty().ifBlank { "your loved one" }
            binding.btnSendGift.text = getString(R.string.gift_sent)
            binding.btnSendGift.setBackgroundResource(R.drawable.bg_button_green)
            Toast.makeText(requireContext(), getString(R.string.gift_ready_message, recipient), Toast.LENGTH_SHORT).show()
            binding.btnSendGift.postDelayed({
                if (_binding != null) {
                    binding.btnSendGift.text = buildSendLabel()
                    binding.btnSendGift.setBackgroundResource(R.drawable.bg_button_gold)
                }
            }, 2200L)
        }

        updateGiftSelection(serviceChips)
        updateOccasionSelection(occasionChips)
    }

    private fun updateGiftSelection(chips: List<TextView>) {
        chips.forEachIndexed { index, chip ->
            val selected = index == selectedGiftIndex
            chip.setBackgroundResource(if (selected) R.drawable.bg_chip_gold_filled else R.drawable.bg_chip_outlined)
            chip.setTextColor(resources.getColor(if (selected) R.color.white else R.color.text_medium, null))
        }

        val gift = DummyContent.giftExperiences[selectedGiftIndex]
        binding.tvGiftPreviewTitle.text = gift.name
        binding.tvGiftPreviewDesc.text = gift.description
        binding.tvGiftPreviewValue.text = "$${gift.price}"
        binding.tvGiftPreviewDuration.text = gift.duration
        binding.tvGiftServiceName.text = gift.name
        binding.tvGiftServicePrice.text = "$${gift.price}"
        binding.tvGiftTotal.text = "$${gift.price}"
        binding.btnSendGift.text = buildSendLabel()
    }

    private fun updateOccasionSelection(chips: List<TextView>) {
        chips.forEachIndexed { index, chip ->
            val selected = index == selectedOccasionIndex
            chip.setBackgroundResource(if (selected) R.drawable.bg_chip_gold_filled else R.drawable.bg_chip_outlined)
            chip.setTextColor(resources.getColor(if (selected) R.color.white else R.color.text_medium, null))
        }
    }

    private fun buildSendLabel(): String {
        val gift = DummyContent.giftExperiences[selectedGiftIndex]
        return "${getString(R.string.send_gift)} · $${gift.price}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
