package com.lustre.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lustre.app.databinding.ActivityPrivacySecurityBinding
import kotlinx.coroutines.launch

class PrivacySecurity: AppCompatActivity() {
    private lateinit var binding: ActivityPrivacySecurityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacySecurityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnUpdatePassword.setOnClickListener {
            binding.btnUpdatePassword.isEnabled = false
            psdChange()
            binding.btnUpdatePassword.isEnabled = true
        }
        binding.btnDeleteAccount.setOnClickListener { confirmDel() }
    }

    private fun psdChange() {
        val current = binding.etCurrentPassword.text.toString()
        val newP = binding.etNewPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()

        if(current.isBlank() || newP.isBlank() || confirm.isBlank()) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
            return
        }
        if(newP.length < 8) {
            Toast.makeText(this, R.string.password_short, Toast.LENGTH_SHORT).show()
            return
        }
        if(newP != confirm) {
            Toast.makeText(this, R.string.passwords_mis_match, Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.changePassword(PasswordChangeReq(current, newP))
                Toast.makeText(this@PrivacySecurity, res.message, Toast.LENGTH_SHORT).show()
            }
            catch (e: Exception) {
                Log.e("Privacy Security", "Password change failed", e)
                Toast.makeText(this@PrivacySecurity, getString(R.string.password_update_failed), Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun confirmDel() {
        AlertDialog.Builder(this).setTitle(R.string.delete_account).setMessage(R.string.delete_account_confirm_message).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete_account) {_,_ ->
            deleteAcc()
        }
            .show()
    }

    private fun deleteAcc() {
        lifecycleScope.launch {
            try {
                ApiClient.apiService.deleteAccount()
                Toast.makeText(this@PrivacySecurity, R.string.account_deleted, Toast.LENGTH_LONG).show()
                ApiClient.clearToken()
                val intent = Intent(this@PrivacySecurity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            catch (e: Exception) {
                Log.e("Privacy Security", "Delete Account Failed", e)
                Toast.makeText(this@PrivacySecurity, R.string.account_delete_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}