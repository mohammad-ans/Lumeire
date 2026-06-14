package com.lumeire.app

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lumeire.app.databinding.FragmentBookingsBinding

class BookingsFragment : Fragment() {

    private var _binding: FragmentBookingsBinding? = null
    private val binding get() = _binding!!
    private val cancelledBookings = mutableSetOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvBookingsCount.text = getString(
            R.string.upcoming_appointments_count,
            DummyContent.upcomingBookings.size
        )

        bindUpcomingCard(
            DummyContent.upcomingBookings[0],
            binding.tvBookingTitle1,
            binding.tvBookingSubtitle1,
            binding.tvBookingMeta1,
            binding.chipBookingStatus1,
            binding.layoutBookingActions1,
            binding.btnBookingCancel1,
            binding.btnBookingReschedule1,
            binding.btnBookingMap1
        )
        bindUpcomingCard(
            DummyContent.upcomingBookings[1],
            binding.tvBookingTitle2,
            binding.tvBookingSubtitle2,
            binding.tvBookingMeta2,
            binding.chipBookingStatus2,
            binding.layoutBookingActions2,
            binding.btnBookingCancel2,
            binding.btnBookingReschedule2,
            binding.btnBookingMap2
        )

        bindPastCard(DummyContent.pastBookings[0], binding.tvPastTitle1, binding.tvPastMeta1)
        bindPastCard(DummyContent.pastBookings[1], binding.tvPastTitle2, binding.tvPastMeta2)
        bindPastCard(DummyContent.pastBookings[2], binding.tvPastTitle3, binding.tvPastMeta3)

        binding.btnBookingReview1.setOnClickListener {
            Toast.makeText(requireContext(), "Reviews are mocked in this app.", Toast.LENGTH_SHORT).show()
        }
        binding.btnBookingAgain1.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.booking_again_message), Toast.LENGTH_SHORT).show()
        }

        binding.btnTabUpcoming.setOnClickListener { showUpcoming(true) }
        binding.btnTabPast.setOnClickListener { showUpcoming(false) }
    }

    private fun bindUpcomingCard(
        booking: Booking,
        titleView: TextView,
        subtitleView: TextView,
        metaView: TextView,
        statusView: TextView,
        actionsLayout: LinearLayout,
        cancelButton: Button,
        rescheduleButton: Button,
        mapButton: Button
    ) {
        titleView.text = booking.service
        subtitleView.text = booking.salon
        metaView.text = booking.schedule + "\n" + booking.details

        cancelButton.setOnClickListener {
            cancelledBookings += booking.id
            statusView.text = getString(R.string.cancelled)
            statusView.setBackgroundResource(R.drawable.bg_cancel_button)
            statusView.setTextColor(resources.getColor(R.color.error_red, null))
            actionsLayout.visibility = View.GONE
            Toast.makeText(requireContext(), getString(R.string.booking_cancelled_message), Toast.LENGTH_SHORT).show()
        }

        rescheduleButton.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.booking_reschedule_message), Toast.LENGTH_SHORT).show()
        }

        mapButton.setOnClickListener {
            (activity as? MainActivity)?.openMaps(booking.salonId)
        }
    }

    private fun bindPastCard(booking: Booking, titleView: TextView, metaView: TextView) {
        titleView.text = "${booking.service} · ${booking.salon}"
        metaView.text = booking.schedule + "\n" + booking.details
    }

    private fun showUpcoming(showUpcoming: Boolean) {
        binding.layoutUpcoming.visibility = if (showUpcoming) View.VISIBLE else View.GONE
        binding.layoutPast.visibility = if (showUpcoming) View.GONE else View.VISIBLE
        binding.btnTabUpcoming.setBackgroundResource(
            if (showUpcoming) R.drawable.bg_tab_selected else 0
        )
        binding.btnTabPast.setBackgroundResource(
            if (showUpcoming) 0 else R.drawable.bg_tab_selected
        )
        if (!showUpcoming) {
            binding.btnTabUpcoming.setBackgroundColor(Color.TRANSPARENT)
        }
        if (showUpcoming) {
            binding.btnTabPast.setBackgroundColor(Color.TRANSPARENT)
        }
        binding.btnTabUpcoming.setTextColor(
            resources.getColor(if (showUpcoming) R.color.gold_dark else R.color.white, null)
        )
        binding.btnTabPast.setTextColor(
            resources.getColor(if (showUpcoming) R.color.white else R.color.gold_dark, null)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
