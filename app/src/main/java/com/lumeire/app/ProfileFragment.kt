package com.lumeire.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.lumeire.app.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Glide.with(this)
            .load(R.drawable.p1)
            .centerCrop()
            .into(binding.ivProfileAvatar)

        listOf(
            binding.rowProfileEdit to getString(R.string.edit_profile),
            binding.rowProfilePayment to getString(R.string.payment_methods),
            binding.rowProfileRewards to getString(R.string.rewards_points),
            binding.rowProfileGifts to getString(R.string.my_gift_cards),
            binding.rowProfilePrivacy to getString(R.string.privacy_security),
            binding.rowProfileSettings to getString(R.string.settings),
            binding.rowProfileHelp to getString(R.string.help_support)
        ).forEach { (viewItem, label) ->
            viewItem.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.section_placeholder_message, label),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked) "Notifications enabled." else "Notifications paused."
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        binding.btnProfileSignOut.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.sign_out_confirm)
                .setMessage(R.string.sign_out_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.sign_out) { _, _ ->
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    activity?.finish()
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
