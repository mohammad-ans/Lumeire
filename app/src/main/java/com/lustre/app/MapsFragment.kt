package com.lustre.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.lustre.app.databinding.FragmentMapsBinding
import com.lustre.app.ui.home.HomeViewModel
import kotlinx.coroutines.launch
import kotlin.getValue
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.lustre.app.data.model.Salon
import androidx.core.net.toUri
import androidx.core.view.isVisible
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapsFragment : Fragment() {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private var selectedSalonId: String = ""
    private lateinit var rows: List<SalonRow>
    private var mapView: MapView? = null
    private var userLocation: GeoPoint? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchUserLocation()
        else Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedSalonId = arguments?.getString(ARG_SELECTED_SALON_ID) ?: ""

        Configuration.getInstance().userAgentValue = requireContext().packageName
        Configuration.getInstance().osmdroidTileCache = requireContext().cacheDir
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
        mapView = binding.map.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.5)
            controller.setCenter(GeoPoint(24, 24))
        }
        if(::rows.isInitialized && rows.isNotEmpty())
            updateMapMarkers()
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        lifecycleScope.launch {
            viewModel.salons.collect { salonList ->
                if (salonList.isEmpty()) return@collect
                applyRows(buildRows(salonList))
            }
        }

        binding.btnMapLocate.setOnClickListener {
            val map = mapView
            val location = userLocation
            if(map != null && location != null){
                map.controller.animateTo(location)
                map.controller.setZoom(14.0)
            }
            else{
                Toast.makeText(requireContext(), "Still locating you...", Toast.LENGTH_SHORT).show()
                fetchUserLocation()
            }
        }
        binding.btnMapCall.setOnClickListener {
            val selected = viewModel.salons.value.firstOrNull {it.id == selectedSalonId}
            val phone = selected?.phone
            if(phone.isNullOrBlank())
                Toast.makeText(requireContext(), "No phone number listed for this salon.", Toast.LENGTH_SHORT).show()
            else{
                val intent = Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())
                startActivity(intent)
            }
        }

        binding.btnMapNavigate.setOnClickListener {
            val selected = rows.firstOrNull { it.salon.id == selectedSalonId }

            when {
                selected == null -> {
                    Toast.makeText(requireContext(), "No salon selected.", Toast.LENGTH_SHORT).show()
                }
                selected.salon.latitude == null || selected.salon.longitude == null -> {
                    Toast.makeText(requireContext(), "Location not available for ${selected.salon.name}.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val gmmIntentUri = "geo:${selected.salon.latitude},${selected.salon.longitude}?q=${Uri.encode(selected.salon.name)}".toUri()
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                        startActivity(mapIntent)
                    } else {
                        Toast.makeText(requireContext(), "Google Maps is not installed.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        binding.etMapSearch.doAfterTextChanged { filterSalons(it?.toString().orEmpty()) }
    }

    private fun buildRows(salonList: List<Salon>): List<SalonRow> {
        val binding = _binding ?: return emptyList()
        val imageViews = listOf(binding.ivSalon1, binding.ivSalon2, binding.ivSalon3, binding.ivSalon4)
        val rowBindings = listOf(
            Triple(binding.rowSalon1, binding.tvSalon1Name, Triple(binding.tvSalon1Category, binding.tvSalon1Meta, binding.tvSalon1Price)),
            Triple(binding.rowSalon2, binding.tvSalon2Name, Triple(binding.tvSalon2Category, binding.tvSalon2Meta, binding.tvSalon2Price)),
            Triple(binding.rowSalon3, binding.tvSalon3Name, Triple(binding.tvSalon3Category, binding.tvSalon3Meta, binding.tvSalon3Price)),
            Triple(binding.rowSalon4, binding.tvSalon4Name, Triple(binding.tvSalon4Category, binding.tvSalon4Meta, binding.tvSalon4Price))
        )
        val sorted = userLocation?.let { origin ->
            salonList.sortedWith(compareBy{ s ->
                if (s.latitude == null || s.longitude == null) Double.MAX_VALUE
                else calculateDistance(origin.latitude, origin.longitude, s.latitude, s.longitude) }
            )
        } ?: salonList

        return sorted.take(4).mapIndexed { index, salonTemp ->
            val row = rowBindings[index % rowBindings.size]
            val distanceStr = userLocation?.let { origin ->
                val lat = salonTemp.latitude ?: return@let "Distance unavailable"
                val lng = salonTemp.longitude ?: return@let "Distance unavailable"
                "${"%.1f".format(calculateDistance(origin.latitude, origin.longitude, lat, lng))} km"
            } ?: "Distance unavailable"

            val salon = Salon(
                id = salonTemp.id,
                name = salonTemp.name,
                category = salonTemp.category ?: "Salon",
                rating = salonTemp.rating,
                review_count = salonTemp.review_count,
                distance=distanceStr,
                address = salonTemp.address,
                closeTime = salonTemp.closeTime ?: "Hours not listed",
                image_url = salonTemp.image_url ?: "",
                latitude = salonTemp.latitude,
                longitude = salonTemp.longitude
            )
            SalonRow(salon, row.first, null, imageViews[index % imageViews.size],
                row.second, row.third.first, row.third.second, row.third.third)
        }
    }

    private fun applyRows(newRows: List<SalonRow>) {
        if (newRows.isEmpty())
            return

        rows = newRows
        rows.forEach { row ->
            bindRow(row)
            row.card.setOnClickListener { updateSelection(row.salon.id) }
        }
        val initialId = if (selectedSalonId.isNotEmpty() && rows.any { it.salon.id == selectedSalonId })
            selectedSalonId
        else
            rows.first().salon.id
        updateSelection(initialId)
        filterSalons("")
    }

    private fun fetchUserLocation() {
        Log.d("MapsFragment", "fetchUserLocation called")

        val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MapsFragment", "Permission NOT granted — returning early")
            return
        }

        Log.d("MapsFragment", "Permission granted — fetching current location")

        val locationRequest = com.google.android.gms.location.CurrentLocationRequest.Builder()
            .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        fusedClient.getCurrentLocation(locationRequest, null)
            .addOnSuccessListener { location ->
                if (location != null && _binding != null) {
                    Log.d("MapsFragment", "Location fetched: lat=${location.latitude}, lng=${location.longitude}")
                    userLocation = GeoPoint(location.latitude, location.longitude)
                    val salonList = viewModel.salons.value
                    if (_binding != null && salonList.isNotEmpty()) {
                        applyRows(buildRows(salonList))
                    }
                    updateMapMarkers()
                } else {
                    Log.d("MapsFragment", "getCurrentLocation returned null")
                }
            }
            .addOnFailureListener { e ->
                Log.e("MapsFragment", "getCurrentLocation failed: ${e.message}")
            }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return (results[0] / 1000.0)
    }
    private fun updateMapMarkers() {
        val map = mapView ?: return
        if (!::rows.isInitialized || rows.isEmpty())
            return
        map.overlays.clear()

        userLocation?.let { p ->
            val marker = Marker(map).apply {
                position = p
                title = "You are here"
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.marker_user)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(marker)
        }

        var selectedLocation: GeoPoint? = null
        
        rows.forEach { row ->
            if (row.card.isVisible) {
                val salon = row.salon
                val lat = salon.latitude ?: return@forEach
                val lng = salon.longitude ?: return@forEach
                val location = GeoPoint(lat, lng)
                val isSelected = salon.id == selectedSalonId
                val iconT = if (isSelected) R.drawable.marker_dest else R.drawable.marker_unselected
                
                val marker = Marker(map).apply {
                    position = location
                    title = salon.name
                    snippet = salon.category
                    icon = ContextCompat.getDrawable(requireContext(), iconT)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)
                if (isSelected) {
                    selectedLocation = location
                }
            }
        }
        map.invalidate()
        selectedLocation?.let {
            map.controller.animateTo(it)
            map.controller.setZoom(14.0)
        }
    }

    private fun bindRow(row: SalonRow) {
        val salon = row.salon
        Glide.with(this).load(salon.image_url).centerCrop().into(row.image)
        row.name.text = salon.name
        row.category.text = salon.category
        row.meta.text = "${salon.distance} · ${getString(R.string.open_until, salon.closeTime)}"
        row.price.text = salon.price
    }

    private fun filterSalons(query: String) {
        if(!::rows.isInitialized)
            return

        val normalized = query.trim().lowercase()
        var visibleCount = 0
        var firstVisibleId: String? = null

        rows.forEach { row ->
            val visible = normalized.isBlank() ||
                row.salon.name.lowercase().contains(normalized) ||
                row.salon.category?.lowercase()?.contains(normalized) ?: false

            row.card.visibility = if (visible) View.VISIBLE else View.GONE
            

            if (visible) {
                visibleCount++
                if (firstVisibleId == null) firstVisibleId = row.salon.id
            }
        }

        binding.tvMapResultCount.text = "$visibleCount ${getString(R.string.salons_nearby)}"

        if (rows.none { it.salon.id == selectedSalonId && it.card.isVisible } && firstVisibleId != null) {
            updateSelection(firstVisibleId)
        } else {
            updateMapMarkers()
        }
    }

    private fun updateSelection(salonId: String) {
        if (salonId.isEmpty() || !::rows.isInitialized || rows.isEmpty())
            return

        val selected = rows.firstOrNull { it.salon.id == salonId } ?: return
        selectedSalonId = salonId

        rows.forEach { row ->
            val active = row.salon.id == salonId
            row.card.strokeWidth = if (active) 2.dp else 1.dp
            row.card.strokeColor = resources.getColor(if (active) R.color.gold_main else R.color.divider, null)
            row.card.setCardBackgroundColor(resources.getColor(if (active) R.color.cream_bg else R.color.white, null))
        }

        Glide.with(this)
            .load(selected.salon.image_url)
            .centerCrop()
            .into(binding.ivMapHighlight)
        binding.tvMapHighlightName.text = selected.salon.name
        binding.tvMapHighlightMeta.text =
            "${selected.salon.address} · ${selected.salon.rating} (${selected.salon.review_count})"

        updateMapMarkers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.overlays?.clear()
        mapView = null
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }
    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    data class SalonRow(
        val salon: Salon,
        val card: MaterialCardView,
        val pinButton: Button?,
        val image: ImageView,
        val name: TextView,
        val category: TextView,
        val meta: TextView,
        val price: TextView
    )

    companion object {
        private const val ARG_SELECTED_SALON_ID = "selected_salon_id"

        fun newInstance(selectedSalonId: String? = null): MapsFragment {
            return MapsFragment().apply {
                arguments = bundleOf(ARG_SELECTED_SALON_ID to (selectedSalonId ?: ""))
            }
        }
    }

}