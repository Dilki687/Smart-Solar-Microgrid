package com.smartsolar.microgrid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.ui.auth.LoginActivity
import com.smartsolar.microgrid.ui.prosumer.ProsumerProfileActivity
import com.smartsolar.microgrid.utils.SessionManager

/**
 * Main application screen.
 *
 * Displays the authenticated user's information and
 * provides role-specific navigation.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        sessionManager =
            SessionManager(applicationContext)

        val user =
            sessionManager.getCurrentUser()

        /*
         * No authenticated user means that the local session
         * is no longer available.
         */
        if (user == null) {

            goToLogin()

            return
        }

        val tvWelcome =
            findViewById<TextView>(R.id.tvWelcome)

        val tvRole =
            findViewById<TextView>(R.id.tvRole)

        val tvUserDetails =
            findViewById<TextView>(R.id.tvUserDetails)

        val btnProfile =
            findViewById<Button>(R.id.btnProfile)

        val btnLogout =
            findViewById<Button>(R.id.btnLogout)

        // Display user information.
        tvWelcome.text =
            "Welcome, ${user.name}"

        tvRole.text =
            "Role: ${formatRole(user.role)}"

        tvUserDetails.text =
            "NIC: ${user.nic}\n" +
                    "Email: ${user.email}\n" +
                    "Status: ${user.status}"

        /*
         * Only Prosumer users can access the Prosumer
         * profile screen.
         */
        if (user.role == "PROSUMER") {

            btnProfile.visibility =
                android.view.View.VISIBLE

            btnProfile.setOnClickListener {

                val intent = Intent(
                    this,
                    ProsumerProfileActivity::class.java
                )

                startActivity(intent)
            }
        }

        // Handle logout.
        btnLogout.setOnClickListener {

            sessionManager.logout()

            goToLogin()
        }
    }

    /**
     * Converts API role names into readable labels.
     */
    private fun formatRole(role: String): String {

        return when (role) {

            "GRID_OPERATOR" ->
                "Grid Operator"

            "BACKOFFICE" ->
                "Backoffice"

            "PROSUMER" ->
                "Prosumer"

            else ->
                role
        }
    }

    /**
     * Opens the login screen.
     */
    private fun goToLogin() {

        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        startActivity(intent)

        finish()
    }
}