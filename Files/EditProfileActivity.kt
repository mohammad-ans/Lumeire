package com.lumeire.app

import android.app.Activity
import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lumeire.app.databinding.ActivityEditProfileBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {
    companion object{
        const val FULL_NAME = "extra_full_name"
        const val PHONE = "extra_phone"
        const val DOB = "extra_dob"
    }
    private lateinit var binding: ActivityEditProfileBinding
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.etFullName.setText(intent.getStringExtra(FULL_NAME) ?: "")
        binding.etPhone.setText(intent.getStringExtra(PHONE) ?: "")
        binding.etDob.setText(intent.getStringExtra(DOB) ?: "")

        binding.etDob.setOnClickListener {datePicker()}
        binding.btnSave.setOnClickListener {saveProfile()}
        binding.btnCancel.setOnClickListener {finish()}
        binding.btnBack.setOnClickListener {finish()}
    }
    private fun datePicker() {
        val calendar = Calendar.getInstance()

        val existing = binding.etDob.text.toString()
        if(existing.isNotBlank()) {
            try{
                dateFormat.parse(existing)?.let { calendar.time = it }
            }
            catch (e: Exception){

            }
        }
        DatePickerDialog(this, {_, year, month, day ->
            val temp = Calendar.getInstance()
            temp.set(year, month, day)
            binding.etDob.setText(dateFormat.format(temp.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    private fun saveProfile(){
        val fullName = binding.etFullName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val dob = binding.etDob.text.toString().trim()
        if(fullName.isEmpty()){
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnSave.isEnabled = false
        binding.btnSave.text = getString(R.string.saving)

        lifecycleScope.launch {
            try {
                ApiClient.apiService.updateProfile(
                    ProfileUpdateRequest(fullName, phone.ifBlank { null }, dob.ifBlank { null })
                )
                setResult(Activity.RESULT_OK)
                finish()
            }
            catch (e: Exception){
                binding.btnSave.isEnabled = true
                binding.btnSave.text = getString(R.string.save)
                Toast.makeText(this@EditProfileActivity, "Could not save changes, please try again", Toast.LENGTH_SHORT).show()
            }
        }
    }
}