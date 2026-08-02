package com.lumeire.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.Api
import kotlinx.coroutines.launch
import com.lumeire.app.databinding.FragmentProfileBinding
import com.lumeire.app.ui.profile.ProfileViewModel

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadAvatar(requireContext(), it) }
    }
    private val editProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if(result.resultCode == Activity.RESULT_OK)
            viewModel.refresh()
    }
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

        lifecycleScope.launch {
            viewModel.profile.collect { profile ->
                Log.d("Profile", "Received: $profile")
                if (profile != null) {
                    binding.tvProfileName.text = profile.full_name ?: "User"
                    binding.tvProfileEmail.text = profile.email
                    binding.tvProfileStatPoints.text = profile.reward_points.toString()
                    binding.tvLoyaltyPointsValue.text = profile.reward_points.toString()
                    binding.tvProfileTierBadge.text = "${profile.loyalty_tier} Member"
                    if (profile.next_tier != null)
                        binding.tvPointsToGold.text = "${profile.points_next_tier} more points to reach ${profile.next_tier}"
                    else
                        binding.tvPointsToGold.text = "You have reached ${profile.loyalty_tier}, max"

                    binding.pbLoyalty.progress = profile.tier_progress.toInt().coerceAtMost(100)
                    val avatar = profile.avatar_url?.let { ApiClient.resolve(profile.avatar_url) }
                    Glide.with(this@ProfileFragment).load(avatar ?: R.drawable.ic_nav_profile)
                        .placeholder(R.drawable.ic_nav_profile)
                        .error(R.drawable.ic_nav_profile)
                        .centerCrop().into(binding.ivProfileAvatar)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.totalBookings.collect { total ->
                binding.tvProfileStatBookings.text = total.toString()
            }
        }
        binding.ivProfileAvatar.setOnClickListener { pickImageLauncher.launch("image/*") }
        listOf(
            binding.rowProfilePayment to getString(R.string.payment_methods),
            binding.rowProfileRewards to getString(R.string.rewards_points),
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
        binding.rowProfileEdit.setOnClickListener {
            val current = viewModel.profile.value
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            intent.putExtra(EditProfileActivity.FULL_NAME, current?.full_name ?: "")
            intent.putExtra(EditProfileActivity.PHONE, current?.phone ?: "")
            intent.putExtra(EditProfileActivity.DOB, current?.data_of_birth ?: "")
            editProfileLauncher.launch(intent)
        }
        binding.rowProfileGifts.setOnClickListener {
            viewModel.fetchGiftCards {giftCards ->
                if(!isAdded)
                    return@fetchGiftCards
                if(giftCards.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "You have no active gift cards",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else {
                    val cardStrings = giftCards.map {"PKR ${it.amount.toInt()}" }.toTypedArray()
                    AlertDialog.Builder(requireContext()).setTitle(R.string.my_gift_cards)
                        .setItems(cardStrings, null)
                        .setPositiveButton("OK", null).show()
                }
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
                    lifecycleScope.launch {
                        ApiClient.clearToken()
                        startActivity(Intent(requireContext(), LoginActivity::class.java))
                        activity?.finish()
                    }
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
