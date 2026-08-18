package com.lustre.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lustre.app.databinding.ActivityHelpBinding
import kotlinx.coroutines.launch

class HelpSupportActivity: AppCompatActivity() {
    private lateinit var binding: ActivityHelpBinding
    private lateinit var ticketAdapter: TicketsAdapter
    private val supportPhone = "+923254518068"
    private val supportEmail = "ansmuhammad340@gmail.com"

    private val faqs = listOf(
        Faq(
            "How do I book an appointment?",
            "Browse a salon, pick a service and stylist, choose a time slot, and confirm from the checkout screen."
        ),
        Faq(
            "How do I cancel or reschedule a booking?",
            "Open the booking from your Bookings list, you can cancel or pick a new time up until a couple of hours before the appointment."
        ),
        Faq(
            "How do reward points work?",
            "You can earn points with every completed booking. Points unlock Bronze, Silver, Gold, and Platinum tiers with better perks, check the Rewards and Points screen for your progress"
        ),
        Faq(
            "How do gift cards work?",
            "Send a gift card to any Lustre user by email from a salon's page. They can redeem it toward a booking at that salon."
        ),
        Faq(
            "How do I change my password?",
            "Go to Profile > Privacy & Security > Change Password. Google signed in account do not have a password to change."
        ),
        Faq(
            "How do I delete my account?",
            "Go to Profile > Privacy & Security > Delete Account. This permanently removes your profile and booking history."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.rvFaqs.layoutManager = LinearLayoutManager(this)
        binding.rvFaqs.adapter = FaqAdapter(faqs)
        ticketAdapter = TicketsAdapter(emptyList())
        binding.rvTickets.layoutManager = LinearLayoutManager(this)
        binding.rvTickets.adapter = ticketAdapter
        loadTickets()
        binding.btnCallSupport.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supportPhone")))
        }
        binding.btnEmailSupport.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$supportEmail"))
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " Support")
            startActivity(Intent.createChooser(intent, getString(R.string.email_us)))
        }
        binding.btnSubmitTicket.setOnClickListener {
            submitTicket()
        }
    }
    private fun loadTickets() {
        lifecycleScope.launch {
            try {
                val tickets = ApiClient.apiService.getTickets()
                if(tickets.isEmpty()) {
                    binding.rvTickets.visibility = View.GONE
                    binding.tvNoTickets.visibility = View.VISIBLE
                }
                else{
                    ticketAdapter.submitList(tickets.sortedByDescending{it.created_at})
                    binding.rvTickets.visibility = View.VISIBLE
                    binding.tvNoTickets.visibility = View.GONE
                }
            }
            catch (_: Exception) {
                Toast.makeText(this@HelpSupportActivity, "Could not load tickets", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun submitTicket() {
        val sbj = binding.etTicketSubject.text.toString().trim()
        val msg = binding.etTicketMessage.text.toString().trim()

        if(sbj.isEmpty() || msg.isEmpty()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnSubmitTicket.isEnabled = false
        lifecycleScope.launch {
            try {
                ApiClient.apiService.createSupportTicket(SupportTicketCreateReq(sbj, msg))
                Toast.makeText(this@HelpSupportActivity, R.string.ticket_submitted, Toast.LENGTH_LONG).show()
                binding.etTicketSubject.text?.clear()
                binding.etTicketMessage.text?.clear()
                loadTickets()
            }
            catch (e: Exception) {
                Log.e("Help Support", "failed to submit ticket", e)
                Toast.makeText(this@HelpSupportActivity, R.string.ticket_submit_failed, Toast.LENGTH_SHORT).show()
            }
            finally {
                binding.btnSubmitTicket.isEnabled = true
            }
        }
    }
}