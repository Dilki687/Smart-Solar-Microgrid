package com.smartsolar.microgrid.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.smartsolar.microgrid.R

/** Displays the branded entry screen before authentication. */
class LandingActivity : AppCompatActivity() {

    private val navigateToLogin = Runnable {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)
        handler.postDelayed(navigateToLogin, 5_000L)
    }

    override fun onDestroy() {
        handler.removeCallbacks(navigateToLogin)
        super.onDestroy()
    }
}
