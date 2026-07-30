package com.lumeire.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lumeire.app.data.model.Booking
import com.lumeire.app.data.model.Service
import com.lumeire.app.data.model.Salon
import kotlinx.coroutines.launch
import java.util.*

class BookingBottomSheet : BottomSheetDialogFragment() {

    private lateinit var salon: Salon
    private var services: List<Service> = emptyList()
    private var selectedService: Service? = null
    private var selectedDateTime: String? = null
    var onBookingConfirmed: (() -> Unit)? = null

    companion object {
        fun newInstance(salon: Salon): BookingBottomSheet {
            return BookingBottomSheet().apply { this.salon = salon }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_booking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvSalonName = view.findViewById<TextView>(R.id.tv_bs_salon_name)
        val spinnerServices = view.findViewById<Spinner>(R.id.spinner_services)
        val btnPickDateTime = view.findViewById<Button>(R.id.btn_pick_datetime)
        val tvSelectedTime = view.findViewById<TextView>(R.id.tv_selected_time)
        val btnConfirm = view.findViewById<Button>(R.id.btn_confirm_booking)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_booking)

        tvSalonName.text = salon.name

        // Fetch services for this salon
        lifecycleScope.launch {
            try {
                services = SupabaseModule.client.postgrest["services"]
                    .select {
                        filter { eq("salon_id", salon.id) }
                    }.decodeList<Service>()

                if (services.isEmpty()) {
                    Toast.makeText(requireContext(), "No services available for this salon", Toast.LENGTH_SHORT).show()
                    dismiss()
                    return@launch
                }

                val serviceNames = services.map { "${it.name} — PKR ${it.price.toInt()} (${it.duration_minutes} min)" }
                spinnerServices.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    serviceNames
                )
                spinnerServices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                        selectedService = services[pos]
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load services", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }

        // Date + Time picker
        btnPickDateTime.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                TimePickerDialog(requireContext(), { _, hour, minute ->
                    selectedDateTime = "%04d-%02d-%02dT%02d:%02d:00".format(year, month + 1, day, hour, minute)
                    tvSelectedTime.text = "%02d/%02d/%04d at %02d:%02d".format(day, month + 1, year, hour, minute)
                    tvSelectedTime.visibility = View.VISIBLE
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Confirm booking
        btnConfirm.setOnClickListener {
            val service = selectedService
            val dateTime = selectedDateTime

            if (service == null) {
                Toast.makeText(requireContext(), "Please select a service", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (dateTime == null) {
                Toast.makeText(requireContext(), "Please select a date and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnConfirm.isEnabled = false

            lifecycleScope.launch {
                try {
                    val userId = SupabaseModule.client.auth.currentSessionOrNull()?.user?.id
                        ?: throw Exception("Not logged in")

                    val booking = mapOf(
                        "user_id" to userId,
                        "salon_id" to salon.id,
                        "appointment_time" to dateTime,
                        "status" to "Upcoming",
                        "total_amount" to service.price
                    )

                    SupabaseModule.client.postgrest["bookings"].insert(booking)

                    Toast.makeText(requireContext(), "Booking confirmed!", Toast.LENGTH_SHORT).show()
                    onBookingConfirmed?.invoke()
                    dismiss()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Booking failed: ${e.message}", Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    btnConfirm.isEnabled = true
                }
            }
        }
    }
}