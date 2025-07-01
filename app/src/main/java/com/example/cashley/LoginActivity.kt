package com.example.cashley

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.regex.Pattern

class LoginActivity : AppCompatActivity() {

    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var signupLink: TextView
    private lateinit var sharedPreferences: SharedPreferences

    // Constants for validation
    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private val EMAIL_PATTERN: Pattern = Patterns.EMAIL_ADDRESS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeViews()
        setupTextWatchers()
        setupClickListeners()
        checkPreviousLogin()
    }

    private fun initializeViews() {
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        emailEditText = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)
        signupLink = findViewById(R.id.signupLink)
        sharedPreferences = getSharedPreferences("cashley", MODE_PRIVATE)
    }

    private fun setupTextWatchers() {
        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateEmail(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validatePassword(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            if (validateForm()) {
                attemptLogin()
            }
        }

        signupLink.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun checkPreviousLogin() {
        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            navigateToMain()
        }
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                emailLayout.error = "Email is required"
                false
            }
            !EMAIL_PATTERN.matcher(email).matches() -> {
                emailLayout.error = "Invalid email format"
                false
            }
            else -> {
                emailLayout.error = null
                true
            }
        }
    }

    private fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                passwordLayout.error = "Password is required"
                false
            }
            password.length < MIN_PASSWORD_LENGTH -> {
                passwordLayout.error = "Password must be at least $MIN_PASSWORD_LENGTH characters"
                false
            }
            else -> {
                passwordLayout.error = null
                true
            }
        }
    }

    private fun validateForm(): Boolean {
        val isEmailValid = validateEmail(emailEditText.text.toString())
        val isPasswordValid = validatePassword(passwordEditText.text.toString())
        return isEmailValid && isPasswordValid
    }

    private fun attemptLogin() {
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()

        val savedEmail = sharedPreferences.getString("email", null)
        val savedPassword = sharedPreferences.getString("password", null)

        if (email == savedEmail && password == savedPassword) {
            loginSuccess()
        } else {
            loginFailure()
        }
    }

    private fun loginSuccess() {
        // Save login state and timestamp
        sharedPreferences.edit().apply {
            putBoolean("isLoggedIn", true)
            putString("lastLoginDate", "2025-04-04 19:43:28")
            putString("currentUser", "")
            apply()
        }

        showToast("Login successful")
        navigateToMain()
    }

    private fun loginFailure() {
        showToast("Invalid email or password")
        passwordEditText.text?.clear()
        passwordEditText.requestFocus()

        // Visual feedback
        passwordLayout.error = "Incorrect password"
        emailLayout.error = "Email not found"

        // Disable login button temporarily
        loginButton.isEnabled = false
        loginButton.postDelayed({
            loginButton.isEnabled = true
            passwordLayout.error = null
            emailLayout.error = null
        }, 2000)
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}