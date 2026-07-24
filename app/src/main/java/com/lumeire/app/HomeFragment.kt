package com.lumeire.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import android.util.Log
import com.lumeire.app.data.model.Salon
import android.widget.LinearLayout
import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.widget.doAfterTextChanged
import com.lumeire.app.di.SupabaseModule
import io.github.jan.supabase.gotrue.auth
import kotlinx.serialization.json.jsonPrimitive

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val viewModel: HomeViewModel by viewModels()
    private val binding get() = _binding!!

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
        lifecycleScope.launch {
            try {
                val user = SupabaseModule.client.auth.currentUserOrNull()
                val fullName = user?.userMetadata
                    ?.get("full_name")?.jsonPrimitive?.content
                    ?: "there"
                binding.homeUsername.text = "Hi, $fullName"
            } catch (e: Exception) {
                binding.homeUsername.text = "Hi there 👋"
            }
        }
        // Using the user-provided profile picture p1 from drawables
        Glide.with(this)
            .load(R.drawable.p1)
            .centerCrop()
            .into(binding.ivHomeAvatar)

        

        binding.etHomeSearch.doAfterTextChanged { filterSalons() }

        listOf(
            binding.chipHaircut,
            binding.chipFacial,
            binding.chipMassage,
            binding.chipNails
        ).forEach { viewItem ->
            viewItem.setOnClickListener {
                val label = (viewItem as TextView).text.toString()
                viewModel.updateCategoryFilter(label)
            }
        }

        binding.tvHomeFilter.setOnClickListener {
            viewModel.updateCategoryFilter(null)
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

        binding.btnHomeNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications are mocked for this app.", Toast.LENGTH_SHORT).show()
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

        val dummyImage = DummyContent.salons.random().imageUrl
        Glide.with(this).load(dummyImage).centerCrop().into(ivSalon)
        tvName.text = salon.name
        tvCategory.text = salon.address ?: "Salon"
        tvMeta.text = "${salon.rating ?: 0.0} (${salon.review_count})"
        tvPrice.text = ""

        cardView.setOnClickListener {
            val sheet = BookingBottomSheet.newInstance(salon)
            sheet.onBookingConfirmed = {
                (activity as? MainActivity)?.openBookings()
            }
            sheet.show(parentFragmentManager, "BookingBottomSheet")
        }

        // Adjust layout params for vertical list
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 0, 0, 14.toInt())
        cardView.layoutParams = params

        return cardView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



