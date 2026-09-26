package com.smartsolar.microgrid

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.ui.auth.LoginActivity
import com.smartsolar.microgrid.ui.backoffice.BackofficeDashboardActivity
import com.smartsolar.microgrid.ui.prosumer.ProsumerDashboardActivity
import com.smartsolar.microgrid.utils.SessionManager

/**
 * Post-login router.
 *
 * Sends each authenticated user to the correct role-specific
 * landing screen. It never has any UI of its own — if the
 * session is missing it hops back to LoginActivity.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(applicationContext)
        val user = sessionManager.getCurrentUser()

        val target = when (user?.role) {
            "PROSUMER" -> ProsumerDashboardActivity::class.java
            "BACKOFFICE" -> BackofficeDashboardActivity::class.java
            "GRID_OPERATOR" -> com.smartsolar.microgrid.ui.operator.OperatorDashboardActivity::class.java
            else -> LoginActivity::class.java
        }

        startActivity(Intent(this, target))
        finish()
    }
}
