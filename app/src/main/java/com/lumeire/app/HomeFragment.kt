package com.lumeire.app

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import androidx.fragment.app.Fragment
import com.lumeire.app.databinding.FragmentHomeBinding
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.lumeire.app.ui.home.HomeViewModel
import kotlinx.coroutines.launch
import android.widget.LinearLayout
import android.view.Gravity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val viewModel: HomeViewModel by viewModels()
    private val binding get() = _binding!!

    private lateinit var chips: List<TextView>
    private var selectedChip: TextView? = null

    private val salonLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res->
        if(res.resultCode == RESULT_OK)
            (activity as? MainActivity)?.openBookings()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvHomeGreeting.text = currentGreeting()
        lifecycleScope.launch {
            try {
                val user = ApiClient.apiService.getMyProfile()
                val fullName = user.full_name ?: "there"
                binding.homeUsername.text = "Hi, $fullName"
                Glide.with(this@HomeFragment)
                    .load(ApiClient.resolve(user.avatar_url))
                    .placeholder(R.drawable.ic_nav_profile)
                    .error(R.drawable.ic_nav_profile)
                    .centerCrop()
                    .into(binding.ivHomeAvatar)
            } catch (e: Exception) {
                binding.homeUsername.text = "Hi there "
            }
        }

        binding.etHomeSearch.doAfterTextChanged { filterSalons() }

        chips = listOf(binding.chipHaircut, binding.chipFacial, binding.chipMassage, binding.chipNails)
        chips.forEach { setChipSelected(it, false) }

        chips.forEach { chip ->
            chip.setOnClickListener {
                if(selectedChip == chip) {
                    setChipSelected(chip, false)
                    selectedChip = null
                    viewModel.updateCategoryFilter(null)
                }
                else {
                    selectedChip?.let { setChipSelected(it, false) }
                    setChipSelected(chip,true)
                    selectedChip = chip
                    viewModel.updateCategoryFilter(chip.text.toString())
                }
            }
        }

        binding.tvHomeFilter.setOnClickListener {
            clearFilter()
        }

        lifecycleScope.launch {
            viewModel.filteredSalons.collect { salons ->
                binding.layoutSalonsContainer.removeAllViews()
                if (salons.isEmpty()) {
                    val tv = TextView(requireContext()).apply {
                        text = "No salons found"
                        gravity = Gravity.CENTER
                        setPadding(0, 50, 0, 50)
                    }
                    binding.layoutSalonsContainer.addView(tv)
                } else {
                    salons.forEach { salon ->
                        val card = createSalonCard(salon)
                        binding.layoutSalonsContainer.addView(card)
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.unreadMsgs.collect { count->
                if(count > 0){
                    binding.tvNotificationBadge.visibility = View.VISIBLE
                    if(count < 10)
                        binding.tvNotificationBadge.text = count.toString()
                    else
                        binding.tvNotificationBadge.text = "9+"
                }
                else
                    binding.tvNotificationBadge.visibility = View.GONE
            }
        }
        binding.btnHomeNotifications.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }
        binding.tvSeeAllServices.setOnClickListener {
            clearFilter()
        }
        binding.tvSeeAllRecommended.setOnClickListener {
            (activity as? MainActivity)?.openMaps()
        }
        binding.cardOfferFirstVisit.setOnClickListener {
            startActivity(Intent(requireContext(), VoucherActivity::class.java))
        }
        binding.cardOfferReferral.setOnClickListener {
            shareReferralCode()
        }
    }

    private fun clearFilter() {
        selectedChip?.let { setChipSelected(it, false) }
        selectedChip = null
        viewModel.updateCategoryFilter(null)
    }
    private fun currentGreeting(): String {
        return when(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> getString(R.string.good_morning)
            in 12..16 -> getString(R.string.good_afternoon)
            in 17..20 -> getString(R.string.good_evening)
            else -> getString(R.string.good_night)
        }
    }
    private fun setChipSelected(chip: TextView, selected: Boolean) {
        if(selected) {
            chip.setBackgroundResource(R.drawable.bg_chip_gold_filled)
            chip.setTextColor(resources.getColor(R.color.white, null))
        }
        else {
            chip.setBackgroundResource(R.drawable.bg_chip_outlined)
            chip.setTextColor(resources.getColor(R.color.text_medium, null))
        }
    }
    private fun shareReferralCode() {
        lifecycleScope.launch {
            try{
                val info = ApiClient.voucherService.getMyReferralInfo()
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, info.share_message)
                }
                startActivity(Intent.createChooser(shareIntent, "Invite a friend"))
            }
            catch (e: Exception) {
                Toast.makeText(requireContext(), "Could not load your referral code, please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun filterSalons() {
        viewModel.updateSearchQuery(binding.etHomeSearch.text.toString())
    }

    private fun createSalonCard(salon: com.lumeire.app.data.model.Salon): View {
        val inflater = LayoutInflater.from(requireContext())
        val cardView = inflater.inflate(R.layout.row_map_salon_content, binding.layoutSalonsContainer, false) as LinearLayout

        val ivSalon = cardView.findViewById<ImageView>(R.id.iv_salon_2)
        val tvName = cardView.findViewById<TextView>(R.id.tv_salon_2_name)
        val tvCategory = cardView.findViewById<TextView>(R.id.tv_salon_2_category)
        val tvMeta = cardView.findViewById<TextView>(R.id.tv_salon_2_meta)
        val tvPrice = cardView.findViewById<TextView>(R.id.tv_salon_2_price)

        Glide.with(this).load(ApiClient.resolve(salon.image_url)).placeholder(R.drawable.ic_nav_map).error(R.drawable.ic_nav_map).centerCrop().into(ivSalon)
        tvName.text = salon.name
        tvCategory.text = salon.category ?: "Salon"
        tvMeta.text = "${salon.rating ?: 0.0} (${salon.review_count})"
        tvPrice.text = ""

        cardView.setOnClickListener {
            salonLauncher.launch(Intent(requireContext(), SalonActivity::class.java).putExtra(
                SalonActivity.EXTRA_SALON_ID, salon.id))
        }

        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 0, 0, 14.toInt())
        cardView.layoutParams = params

        return cardView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}



