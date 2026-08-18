package com.lustre.app

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
import kotlinx.coroutines.launch
import com.lustre.app.databinding.FragmentProfileBinding
import com.lustre.app.ui.profile.ProfileViewModel

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
        binding.rowProfilePayment.setOnClickListener {
                Toast.makeText(requireContext(), getString(R.string.section_placeholder_message, "Payment Methods"),Toast.LENGTH_SHORT).show()
        }
        binding.rowProfileRewards.setOnClickListener {
            startActivity(Intent(requireContext(), RewardsActivity::class.java))
        }
        binding.rowProfileHelp.setOnClickListener {
            startActivity(Intent(requireContext(), HelpSupportActivity::class.java))
        }
        binding.rowProfilePrivacy.setOnClickListener {
            startActivity(Intent(requireContext(), PrivacySecurity::class.java))
        }
        binding.rowProfileSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        binding.rowProfileEdit.setOnClickListener {
            val current = viewModel.profile.value
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            intent.putExtra(EditProfileActivity.FULL_NAME, current?.full_name ?: "")
            intent.putExtra(EditProfileActivity.PHONE, current?.phone ?: "")
            intent.putExtra(EditProfileActivity.DOB, current?.date_of_birth ?: "")
            editProfileLauncher.launch(intent)
        }
        binding.rowProfileGifts.setOnClickListener {
            startActivity(Intent(requireContext(), GiftCardActivity::class.java))
        }

        binding.switchNotifications.isChecked = PushPreferences.getPush(requireContext())
        binding.switchNotifications.setOnCheckedChangeListener(null)
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            PushPreferences.setPushed(requireContext(), isChecked, lifecycleScope)
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
