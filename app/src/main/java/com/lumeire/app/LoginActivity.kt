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
import com.lumeire.app.databinding.ActivityLoginBinding
import com.lumeire.app.ui.login.LoginState
import com.lumeire.app.ui.login.LoginViewModel
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            if(ApiClient.isLoggedIn()) {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
                return@launch
            }
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(email, password)
            } else {
                Toast.makeText(this, getString(R.string.login_missing_fields), Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is LoginState.Loading -> {
                        binding.btnLogin.isEnabled = false
                        binding.btnLogin.text = "Loading..."
                    }
                    is LoginState.Success -> {
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                    is LoginState.Error -> {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = getString(R.string.sign_in)
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    is LoginState.Idle -> {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = getString(R.string.sign_in)
                    }
                }
            }
        }

        binding.btnGoogleLogin.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.CLIENT_ID)
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
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







