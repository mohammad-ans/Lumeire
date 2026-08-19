package com.lustre.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lustre.app.databinding.FragmentBookingsBinding
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.lustre.app.ui.bookings.BookingsViewModel
import kotlinx.coroutines.launch
import com.lustre.app.data.model.Booking
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

        binding.btnTabUpcoming.setOnClickListener { showUpcoming(true) }
        binding.btnTabPast.setOnClickListener { showUpcoming(false) }

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
        val parent = if(isUpcoming) binding.layoutUpcoming else binding.layoutPast

        val contentRow = inflater.inflate(R.layout.row_map_salon_content, parent, false) as LinearLayout

        val tvName = contentRow.findViewById<TextView>(R.id.tv_salon_2_name)
        val tvMeta = contentRow.findViewById<TextView>(R.id.tv_salon_2_meta)
        val tvCategory = contentRow.findViewById<TextView>(R.id.tv_salon_2_category)

        tvName.text = "Booking #${booking.id.takeLast(4)}"
        tvCategory.text = "Salon ID: ${booking.salon_id}"
        tvMeta.text = "Status: ${booking.status}\nPayment: ${booking.payment_status} (${booking.currency} ${booking.total_amount.toInt()})\nTime: ${booking.appointment_time}"

        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_white)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0,0,0,14)
            }
        }
        card.addView(contentRow)
        val upcoming = isUpcoming && booking.status == "Upcoming"
        val canPay = booking.payment_status == "unpaid" && booking.status != "Cancelled"

        if (canPay) {
                val payBtn = Button(requireContext()).apply {
                    text = "Pay Now"
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.bg_button_green)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(24,0,24,8)
                    }
                    setOnClickListener { openPayment(booking) }
                }
                card.addView(payBtn)
            }

            if(upcoming) {
                val actionRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(24,0,24,16)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                val rescheduleBtn = Button(requireContext()).apply {
                    text = "Reschedule"
                    setTextColor(resources.getColor(R.color.gold_dark, null))
                    setBackgroundColor(Color.TRANSPARENT)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener { rescheduleDialog(booking) }
                }
                actionRow.addView(rescheduleBtn)
                val cancelBtn = Button(requireContext()).apply {
                    text = "Cancel"
                    setTextColor(Color.DKGRAY)
                    setBackgroundResource(R.drawable.bg_cancel_button)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = 12
                    }
                    setOnClickListener { cancelBooking(booking) }
                }
                actionRow.addView(cancelBtn)
                card.addView(actionRow)
            }
        return card
    }

    private fun cancelBooking(booking: Booking) {
        AlertDialog.Builder(requireContext()).setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking? This cannot be undone.")
            .setNegativeButton("Keep booking", null)
            .setPositiveButton("Cancel Booking") {_,_ ->
                viewModel.cancelBooking(booking.id)
            }.show()
    }

    private fun openPayment(booking: Booking) {
        val intent = Intent(requireContext(), PaymentActivity::class.java).apply {
            putExtra(PaymentActivity.EXTRA_BOOKING_ID, booking.id)
            putExtra(PaymentActivity.EXTRA_SALON_ID, booking.salon_id)
            putExtra(PaymentActivity.EXTRA_AMOUNT, booking.total_amount)
            putExtra(PaymentActivity.EXTRA_CURRENCY, booking.currency)
        }
        startActivity(intent)
    }

    private fun rescheduleDialog(booking: Booking) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), {_, year, month,day ->
            TimePickerDialog(requireContext(), {_,hour,minute ->
                val iso = "%04d-%02d-%02dT%02d:%02d:00".format(year, month + 1, day, hour, minute)
                val displayDateTime = "%02d/%02d/%04d at %02d:%02d".format(day, month + 1, year, hour, minute)
                AlertDialog.Builder(requireContext()).setTitle("Reschedule Booking").setMessage("Move this booking to $displayDateTime")
                    .setNegativeButton("Cancel", null).setPositiveButton("Confirm") {_,_ ->
                        viewModel.rescheduleBooking(booking.id, iso, ::f)
                    }.show()
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    private fun f(success: Boolean, msg: String?) {
        if(success)
            Toast.makeText(requireContext(), "Booking rescheduled", Toast.LENGTH_SHORT).show()
        else
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
    private fun showUpcoming(showUpcoming: Boolean) {
        binding.layoutUpcoming.visibility = if (showUpcoming) View.VISIBLE else View.GONE
        binding.layoutPast.visibility = if (showUpcoming) View.GONE else View.VISIBLE

        val selectedColor = resources.getColor(R.color.gold_dark, null)
        val unselectedColor = Color.WHITE

        binding.btnTabUpcoming.apply {
            setBackgroundResource(if (showUpcoming) R.drawable.bg_tab_selected else 0)
            if (!showUpcoming)
                setBackgroundColor(Color.TRANSPARENT)
            setTextColor(if (showUpcoming) selectedColor else unselectedColor)
        }

        binding.btnTabPast.apply {
            setBackgroundResource(if (!showUpcoming) R.drawable.bg_tab_selected else 0)
            if (showUpcoming)
                setBackgroundColor(Color.TRANSPARENT)
            setTextColor(if (!showUpcoming) selectedColor else unselectedColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}