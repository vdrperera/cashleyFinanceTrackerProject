package com.example.cashley

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cashley.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.regex.Pattern

class SignupActivity : AppCompatActivity() {

    private lateinit var emailLayout: TextInputLayout
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout

    private lateinit var emailEditText: TextInputEditText
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText

    private lateinit var signupButton: MaterialButton
    private lateinit var loginLink: TextView
    private lateinit var sharedPreferences: SharedPreferences

    // Constants for validation
    companion object {
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MIN_USERNAME_LENGTH = 3
        private val EMAIL_PATTERN: Pattern = Patterns.EMAIL_ADDRESS
        private val USERNAME_PATTERN: Pattern = Pattern.compile("^[a-zA-Z0-9_]+$")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        initializeViews()
        setupTextWatchers()
        setupClickListeners()
    }

    private fun initializeViews() {
        emailLayout = findViewById(R.id.emailLayout)
        usernameLayout = findViewById(R.id.usernameLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)

        emailEditText = findViewById(R.id.email)
        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        confirmPasswordEditText = findViewById(R.id.confirmPassword)

        signupButton = findViewById(R.id.signupButton)
        loginLink = findViewById(R.id.loginLink)
        sharedPreferences = getSharedPreferences("cashley", MODE_PRIVATE)
    }

    private fun setupTextWatchers() {
        emailEditText.addTextChangedListener(createTextWatcher { validateEmail(it) })
        usernameEditText.addTextChangedListener(createTextWatcher { validateUsername(it) })
        passwordEditText.addTextChangedListener(createTextWatcher {
            validatePassword(it)
            validateConfirmPassword(confirmPasswordEditText.text.toString())
        })
        confirmPasswordEditText.addTextChangedListener(createTextWatcher {
            validateConfirmPassword(
                it
            )
        })
    }

    private fun createTextWatcher(validationFunction: (String) -> Boolean): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validationFunction(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        }
    }

    private fun setupClickListeners() {
        signupButton.setOnClickListener {
            if (validateForm()) {
                attemptSignup()
            }
        }

        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
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

            sharedPreferences.getString("email", null) == email -> {
                emailLayout.error = "Email already registered"
                false
            }

            else -> {
                emailLayout.error = null
                true
            }
        }
    }

    private fun validateUsername(username: String): Boolean {
        return when {
            username.isEmpty() -> {
                usernameLayout.error = "Username is required"
                false
            }

            username.length < MIN_USERNAME_LENGTH -> {
                usernameLayout.error = "Username must be at least $MIN_USERNAME_LENGTH characters"
                false
            }

            !USERNAME_PATTERN.matcher(username).matches() -> {
                usernameLayout.error = "Username can only contain letters, numbers, and underscores"
                false
            }

            sharedPreferences.getString("username", null) == username -> {
                usernameLayout.error = "Username already taken"
                false
            }

            else -> {
                usernameLayout.error = null
                true
            }
        }
    }

    private fun validatePassword(password: String): Boolean {
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        return when {
            password.isEmpty() -> {
                passwordLayout.error = "Password is required"
                false
            }

            password.length < MIN_PASSWORD_LENGTH -> {
                passwordLayout.error = "Password must be at least $MIN_PASSWORD_LENGTH characters"
                false
            }

            !hasUpperCase -> {
                passwordLayout.error = "Password must contain at least one uppercase letter"
                false
            }

            !hasLowerCase -> {
                passwordLayout.error = "Password must contain at least one lowercase letter"
                false
            }

            !hasDigit -> {
                passwordLayout.error = "Password must contain at least one number"
                false
            }

            !hasSpecialChar -> {
                passwordLayout.error = "Password must contain at least one special character"
                false
            }

            else -> {
                passwordLayout.error = null
                updatePasswordStrengthIndicator(password)
                true
            }
        }
    }

    private fun validateConfirmPassword(confirmPassword: String): Boolean {
        return when {
            confirmPassword.isEmpty() -> {
                confirmPasswordLayout.error = "Please confirm your password"
                false
            }

            confirmPassword != passwordEditText.text.toString() -> {
                confirmPasswordLayout.error = "Passwords do not match"
                false
            }

            else -> {
                confirmPasswordLayout.error = null
                true
            }
        }
    }

    private fun updatePasswordStrengthIndicator(password: String) {
        val strength = calculatePasswordStrength(password)
        val helperText = when {
            strength >= 80 -> "Strong password"
            strength >= 60 -> "Good password"
            strength >= 40 -> "Moderate password"
            else -> "Weak password"
        }
        passwordLayout.helperText = helperText
    }

    private fun calculatePasswordStrength(password: String): Int {
        var score = 0
        if (password.length >= MIN_PASSWORD_LENGTH) score += 20
        if (password.any { it.isUpperCase() }) score += 20
        if (password.any { it.isLowerCase() }) score += 20
        if (password.any { it.isDigit() }) score += 20
        if (password.any { !it.isLetterOrDigit() }) score += 20
        return score
    }

    private fun validateForm(): Boolean {
        val isEmailValid = validateEmail(emailEditText.text.toString())
        val isUsernameValid = validateUsername(usernameEditText.text.toString())
        val isPasswordValid = validatePassword(passwordEditText.text.toString())
        val isConfirmPasswordValid =
            validateConfirmPassword(confirmPasswordEditText.text.toString())

        return isEmailValid && isUsernameValid && isPasswordValid && isConfirmPasswordValid
    }

    private fun attemptSignup() {
        val email = emailEditText.text.toString()
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()

        sharedPreferences.edit().apply {
            putString("email", email)
            putString("username", username)
            putString("password", password)
            putString("registrationDate", "2025-04-04 20:18:12") // Current timestamp
            // Remove these lines as they shouldn't be set during signup
            // putString("lastLoginDate", "2025-04-04 20:18:12")
            // putBoolean("isLoggedIn", true)
            apply()
        }

        showSuccessAndNavigate()
    }

    private fun showSuccessAndNavigate() {
        Toast.makeText(this, "Account created successfully! Please login.", Toast.LENGTH_LONG)
            .show()
        // Navigate to LoginActivity instead of MainActivity
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}