package com.smartsolar.microgrid.ui.operator

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartsolar.microgrid.data.local.UserDao
import com.smartsolar.microgrid.data.repository.AuthRepository
import com.smartsolar.microgrid.data.repository.OperatorApiException
import com.smartsolar.microgrid.ui.auth.LoginActivity
import com.smartsolar.microgrid.utils.SessionManager
import kotlinx.coroutines.launch

abstract class OperatorBaseActivity : AppCompatActivity() {
    protected fun sessionValid(): Boolean {
        val session = SessionManager(applicationContext)
        if (!session.isLoggedIn() || session.getUserRole() != "GRID_OPERATOR") {
            login()
            return false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        sessionValid()
    }

    protected fun showFailure(error: Throwable) {
        Toast.makeText(this, error.message ?: "Action failed. Please try again.", Toast.LENGTH_LONG).show()
        if ((error as? OperatorApiException)?.status == 401) {
            SessionManager(applicationContext).logout()
            login()
        }
    }

    protected fun logout() {
        lifecycleScope.launch {
            AuthRepository(UserDao(applicationContext)).logoutFromServer()
            login()
        }
    }

    private fun login() {
        startActivity(Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
