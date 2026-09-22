package com.smartsolar.microgrid.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartsolar.microgrid.MainActivity
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.local.UserDao
import com.smartsolar.microgrid.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * Login screen for the Smart Solar Microgrid mobile application.
 *
 * Handles:
 * - NIC/email and password input
 * - API authentication
 * - JWT/session storage
 * - Role-based navigation
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var identifierInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display login layout.
        setContentView(R.layout.activity_login)

        // Initialize UI components.
        identifierInput = findViewById(R.id.etIdentifier)
        passwordInput = findViewById(R.id.etPassword)
        loginButton = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.tvError)

        // Initialize authentication repository.
        val userDao = UserDao(applicationContext)

        authRepository = AuthRepository(userDao)

        // Check whether a previous login session exists.
        checkExistingSession()

        // Handle login button click.
        loginButton.setOnClickListener {
            performLogin()
        }
    }

    /**
     * Checks whether the user is already authenticated locally.
     */
    private fun checkExistingSession() {

        val currentUser = authRepository.getCurrentUser()
        val token = authRepository.getToken()

        if (currentUser != null && !token.isNullOrBlank()) {

            /*
             * A valid local session exists.
             *
             * Continue to the main application screen.
             */
            navigateToMainActivity()
        }
    }

    /**
     * Performs authentication using the central C# API.
     */
    private fun performLogin() {

        // Clear previous error.
        errorText.visibility = View.GONE

        // Read input values.
        val identifier = identifierInput.text.toString().trim()
        val password = passwordInput.text.toString()

        // Validate identifier.
        if (identifier.isEmpty()) {

            identifierInput.error = "Enter your NIC or email"
            identifierInput.requestFocus()
            return
        }

        // Validate password.
        if (password.isEmpty()) {

            passwordInput.error = "Enter your password"
            passwordInput.requestFocus()
            return
        }

        // Display loading state.
        setLoadingState(true)

        /*
         * Launch network operation using Kotlin coroutine.
         */
        lifecycleScope.launch {

            val result = authRepository.login(
                identifier = identifier,
                password = password
            )

            // Stop loading.
            setLoadingState(false)

            result.onSuccess { loginResponse ->

                Toast.makeText(
                    this@LoginActivity,
                    loginResponse.message,
                    Toast.LENGTH_SHORT
                ).show()

                /*
                 * Authentication succeeded.
                 *
                 * The user and JWT have already been saved
                 * by AuthRepository.
                 */
                navigateToMainActivity()
            }

            result.onFailure { exception ->

                showError(
                    exception.message
                        ?: "Login failed. Please try again."
                )
            }
        }
    }

    /**
     * Enables/disables the login loading state.
     */
    private fun setLoadingState(isLoading: Boolean) {

        loginButton.isEnabled = !isLoading
        identifierInput.isEnabled = !isLoading
        passwordInput.isEnabled = !isLoading

        progressBar.visibility =
            if (isLoading) View.VISIBLE else View.GONE

        loginButton.text =
            if (isLoading) "Logging in..." else "LOGIN"
    }

    /**
     * Displays an authentication error.
     */
    private fun showError(message: String) {

        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    /**
     * Opens the main application screen.
     */
    private fun navigateToMainActivity() {

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        startActivity(intent)

        finish()
    }
}