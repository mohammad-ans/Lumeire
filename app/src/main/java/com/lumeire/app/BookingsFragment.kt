package com.lumeire.app

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lumeire.app.databinding.FragmentBookingsBinding
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.lumeire.app.ui.bookings.BookingsViewModel
import kotlinx.coroutines.launch
import com.lumeire.app.data.model.Booking
import androidx.appcompat.app.AlertDialog

class BookingsFragment : Fragment() {

    private var _binding: FragmentBookingsBinding? = null
    private val viewModel: BookingsViewModel by viewModels()
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Tab Click Listeners
        binding.btnTabUpcoming.setOnClickListener { showUpcoming(true) }
        binding.btnTabPast.setOnClickListener { showUpcoming(false) }

        // Observe Bookings for Upcoming Tab
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bookings.collect { bookingsList ->
                binding.layoutUpcoming.removeAllViews()
                val upcoming = bookingsList.filter { it.status != "Done" && it.status != "Cancelled" }
                binding.tvBookingsCount.text = getString(R.string.upcoming_appointments_count, upcoming.size)

                if (upcoming.isEmpty()) {
                    val tv = TextView(requireContext()).apply {
                        text = "No upcoming bookings found"
                        gravity = android.view.Gravity.CENTER
                        setPadding(0, 50, 0, 50)
                    }
                    binding.layoutUpcoming.addView(tv)
                } else {
                    upcoming.forEach { booking ->
                        val card = createBookingCard(booking, true)
                        binding.layoutUpcoming.addView(card)
                    }
                }
            }
        }

        // Observe Bookings for Past Tab
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bookings.collect { bookingsList ->
                binding.layoutPast.removeAllViews()
                val past = bookingsList.filter { it.status == "Done" || it.status == "Cancelled" }

                if (past.isEmpty()) {
                    val tv = TextView(requireContext()).apply {
                        text = "No past bookings found"
                        gravity = android.view.Gravity.CENTER
                        setPadding(0, 50, 0, 50)
                    }
                    binding.layoutPast.addView(tv)
                } else {
                    past.forEach { booking ->
                        val card = createBookingCard(booking, false)
                        binding.layoutPast.addView(card)
                    }
                }
            }
        }
    }


    private fun createBookingCard(booking: Booking, isUpcoming: Boolean): View {
        val inflater = LayoutInflater.from(requireContext())
        val cardView = inflater.inflate(R.layout.row_map_salon_content, if(isUpcoming) binding.layoutUpcoming else binding.layoutPast, false) as com.google.android.material.card.MaterialCardView

        val tvName = cardView.findViewById<TextView>(R.id.tv_salon_2_name)
        val tvMeta = cardView.findViewById<TextView>(R.id.tv_salon_2_meta)
        val tvCategory = cardView.findViewById<TextView>(R.id.tv_salon_2_category)

        tvName.text = "Booking #${booking.id.takeLast(4)}"
        tvCategory.text = "Salon ID: ${booking.salon_id}"
        tvMeta.text = "Status: ${booking.status}\nPayment: ${booking.payment_status} (${booking.currency} ${booking.total_amount.toInt()})\nTime: ${booking.appointment_time}"

        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 0, 0, 14)
        cardView.layoutParams = params
        return cardView
    }

    private fun showUpcoming(showUpcoming: Boolean) {
        binding.layoutUpcoming.visibility = if (showUpcoming) View.VISIBLE else View.GONE
        binding.layoutPast.visibility = if (showUpcoming) View.GONE else View.VISIBLE

        val selectedColor = resources.getColor(R.color.gold_dark, null)
        val unselectedColor = Color.WHITE

        binding.btnTabUpcoming.apply {
            setBackgroundResource(if (showUpcoming) R.drawable.bg_tab_selected else 0)
            if (!showUpcoming) setBackgroundColor(Color.TRANSPARENT)
            setTextColor(if (showUpcoming) selectedColor else unselectedColor)
        }

        binding.btnTabPast.apply {
            setBackgroundResource(if (!showUpcoming) R.drawable.bg_tab_selected else 0)
            if (showUpcoming) setBackgroundColor(Color.TRANSPARENT)
            setTextColor(if (!showUpcoming) selectedColor else unselectedColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun showSalonPickerDialog() {
        viewModel.fetchSalons { salons ->
            if (salons.isEmpty()) {
                Toast.makeText(requireContext(), "No salons available", Toast.LENGTH_SHORT).show()
                return@fetchSalons
            }
            val names = salons.map { it.name }.toTypedArray()
            requireActivity().runOnUiThread {
                AlertDialog.Builder(requireContext())
                    .setTitle("Select a Salon")
                    .setItems(names) { _, index ->
                        val sheet = BookingBottomSheet.newInstance(salons[index])
                        sheet.onBookingConfirmed = { viewModel.refresh() }
                        sheet.show(childFragmentManager, "BookingBottomSheet")
                    }
                    .show()
            }
        }
    }
}