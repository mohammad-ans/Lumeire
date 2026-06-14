package com.lumeire.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.lumeire.app.databinding.FragmentMapsBinding

class MapsFragment : Fragment() {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!
    private var selectedSalonId: Int = 1
    private lateinit var rows: List<SalonRow>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedSalonId = arguments?.getInt(ARG_SELECTED_SALON_ID) ?: 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rows = listOf(
            SalonRow(DummyContent.salons[0], binding.rowSalon1, binding.btnPin1, binding.ivSalon1, binding.tvSalon1Name, binding.tvSalon1Category, binding.tvSalon1Meta, binding.tvSalon1Price),
            SalonRow(DummyContent.salons[1], binding.rowSalon2, binding.btnPin2, binding.ivSalon2, binding.tvSalon2Name, binding.tvSalon2Category, binding.tvSalon2Meta, binding.tvSalon2Price),
            SalonRow(DummyContent.salons[2], binding.rowSalon3, binding.btnPin3, binding.ivSalon3, binding.tvSalon3Name, binding.tvSalon3Category, binding.tvSalon3Meta, binding.tvSalon3Price),
            SalonRow(DummyContent.salons[3], binding.rowSalon4, binding.btnPin4, binding.ivSalon4, binding.tvSalon4Name, binding.tvSalon4Category, binding.tvSalon4Meta, binding.tvSalon4Price)
        )

        rows.forEach { row ->
            bindRow(row)
            row.card.setOnClickListener { updateSelection(row.salon.id) }
            row.pinButton.setOnClickListener { updateSelection(row.salon.id) }
        }

        binding.btnMapLocate.setOnClickListener {
            Toast.makeText(requireContext(), "Location is mocked around the city center.", Toast.LENGTH_SHORT).show()
        }
        binding.btnMapCall.setOnClickListener {
            Toast.makeText(requireContext(), "Calling is not connected in this dummy app.", Toast.LENGTH_SHORT).show()
        }
        binding.btnMapNavigate.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.map_navigate_message), Toast.LENGTH_SHORT).show()
        }
        binding.etMapSearch.doAfterTextChanged { filterSalons(it?.toString().orEmpty()) }

        updateSelection(selectedSalonId)
        filterSalons("")
    }

    private fun bindRow(row: SalonRow) {
        val salon = row.salon
        Glide.with(this).load(salon.imageUrl).centerCrop().into(row.image)
        row.name.text = salon.name
        row.category.text = salon.category
        row.meta.text = "${salon.distance} · ${getString(R.string.open_until, salon.openUntil)}"
        row.price.text = salon.price
    }

    private fun filterSalons(query: String) {
        val normalized = query.trim().lowercase()
        var visibleCount = 0
        var firstVisibleId: Int? = null

        rows.forEach { row ->
            val visible = normalized.isBlank() ||
                row.salon.name.lowercase().contains(normalized) ||
                row.salon.category.lowercase().contains(normalized)

            row.card.visibility = if (visible) View.VISIBLE else View.GONE
            row.pinButton.visibility = if (visible) View.VISIBLE else View.GONE

            if (visible) {
                visibleCount++
                if (firstVisibleId == null) firstVisibleId = row.salon.id
            }
        }

        binding.tvMapResultCount.text = "$visibleCount ${getString(R.string.salons_nearby)}"

        if (rows.none { it.salon.id == selectedSalonId && it.card.visibility == View.VISIBLE } && firstVisibleId != null) {
            updateSelection(firstVisibleId!!)
        }
    }

    private fun updateSelection(salonId: Int) {
        selectedSalonId = salonId
        val selected = rows.first { it.salon.id == salonId }

        rows.forEach { row ->
            val active = row.salon.id == salonId
            row.card.strokeWidth = if (active) 2.dp else 1.dp
            row.card.strokeColor = resources.getColor(if (active) R.color.gold_main else R.color.divider, null)
            row.card.setCardBackgroundColor(resources.getColor(if (active) R.color.cream_bg else R.color.white, null))
            row.pinButton.setBackgroundResource(if (active) R.drawable.bg_map_pin_selected else R.drawable.bg_map_pin_default)
            row.pinButton.setTextColor(resources.getColor(if (active) R.color.white else R.color.text_dark, null))
        }

        Glide.with(this)
            .load(selected.salon.imageUrl)
            .centerCrop()
            .into(binding.ivMapHighlight)
        binding.tvMapHighlightName.text = selected.salon.name
        binding.tvMapHighlightMeta.text =
            "${selected.salon.address} · ${selected.salon.rating} (${selected.salon.reviews})"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    data class SalonRow(
        val salon: Salon,
        val card: MaterialCardView,
        val pinButton: Button,
        val image: ImageView,
        val name: TextView,
        val category: TextView,
        val meta: TextView,
        val price: TextView
    )

    companion object {
        private const val ARG_SELECTED_SALON_ID = "selected_salon_id"

        fun newInstance(selectedSalonId: Int? = null): MapsFragment {
            return MapsFragment().apply {
                arguments = bundleOf(ARG_SELECTED_SALON_ID to (selectedSalonId ?: 1))
            }
        }
    }
}
