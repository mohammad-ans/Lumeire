package com.lumeire.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lumeire.app.databinding.ActivitySignupBinding
import com.lumeire.app.ui.login.LoginState
import com.lumeire.app.ui.login.LoginViewModel
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.lumeire.app.data.model.Profile
import com.lumeire.app.ui.login.OtpState

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignup.setOnClickListener {
            val fullname = binding.etFullname.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            Log.d("User name: ", "$fullname")
            if (fullname.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                if (password == binding.etConfirmPassword.text.toString()) {
                    viewModel.register(email, password, fullname)
                }
                else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, getString(R.string.login_missing_fields), Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            viewModel.otpState.collect { state ->
                when (state) {
                    is OtpState.Loading -> {
                        binding.btnSignup.isEnabled = false
                        binding.btnSignup.text = "Loading..."
                    }
                    is OtpState.CodeSent -> {
                        binding.btnSignup.isEnabled = true
                        binding.btnSignup.text = getString(R.string.sign_in)
                        startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                        finish()
                    }
                    is OtpState.Error -> {
                        binding.btnSignup.isEnabled = true
                        binding.btnSignup.text = getString(R.string.sign_in)
                        Toast.makeText(this@SignupActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is OtpState.Idle -> {
                        binding.btnSignup.isEnabled = true
                        binding.btnSignup.text = getString(R.string.sign_in)
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.loginState.collect {state ->
                when(state) {
                    is LoginState.Success -> {
                        startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                        finish()
                    }
                    is LoginState.Error -> {
                        Toast.makeText(this@SignupActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> Unit
                }
            }
        }

        binding.btnGoogleLogin.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.CLIENT_ID)
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.tvSignin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

                try {
                    val account = task.getResult(ApiException::class.java)
                    val idTok = account.idToken

                    if (idTok != null) {
                        viewModel.googleAuth(idTok)
                    }
                } catch (e: ApiException) {
                    Log.e("Google Login", "Google sign-in failed", e)
                    Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_LONG).show()
                }
            }
        }

}



