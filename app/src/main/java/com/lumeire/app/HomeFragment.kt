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

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
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

        // Using the user-provided profile picture p1 from drawables
        Glide.with(this)
            .load(R.drawable.p1)
            .centerCrop()
            .into(binding.ivHomeAvatar)

        val featuredSalons = DummyContent.salons.take(3)

        bindSalon(
            featuredSalons[0],
            binding.ivHomeSalon1,
            binding.tvHomeSalon1Name,
            binding.tvHomeSalon1Meta,
            binding.tvHomeSalon1Price,
            binding.btnHomeBook1
        )
        bindSalon(
            featuredSalons[1],
            binding.ivHomeSalon2,
            binding.tvHomeSalon2Name,
            binding.tvHomeSalon2Meta,
            binding.tvHomeSalon2Price,
            binding.btnHomeBook2
        )
        bindSalon(
            featuredSalons[2],
            binding.ivHomeSalon3,
            binding.tvHomeSalon3Name,
            binding.tvHomeSalon3Meta,
            binding.tvHomeSalon3Price,
            binding.btnHomeBook3
        )

        listOf(
            binding.chipHaircut,
            binding.chipFacial,
            binding.chipMassage,
            binding.chipNails,
            binding.tvHomeFilter
        ).forEach { viewItem ->
            viewItem.setOnClickListener {
                val label = if (viewItem is TextView) viewItem.text else "Feature"
                Toast.makeText(requireContext(), "$label is part of the dummy flow.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnHomeNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications are mocked for this app.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindSalon(
        salon: Salon,
        imageView: ImageView,
        nameView: TextView,
        metaView: TextView,
        priceView: TextView,
        actionButton: Button
    ) {
        Glide.with(this)
            .load(salon.imageUrl)
            .centerCrop()
            .into(imageView)

        nameView.text = salon.name
        metaView.text = "${salon.category} · ${salon.rating} (${salon.reviews}) · ${salon.distance}"
        priceView.text = salon.price
        actionButton.setOnClickListener {
            (activity as? MainActivity)?.openBookings()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
