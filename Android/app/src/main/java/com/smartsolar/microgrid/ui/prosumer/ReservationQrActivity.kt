package com.smartsolar.microgrid.ui.prosumer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.OperatorApiException
import com.smartsolar.microgrid.data.repository.OperatorRepository
import com.smartsolar.microgrid.ui.auth.LoginActivity
import com.smartsolar.microgrid.utils.ReservationFormat
import com.smartsolar.microgrid.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Displays only the server-issued opaque QR. The API checks ownership and reservation state. */
class ReservationQrActivity : AppCompatActivity() {
    private var loading = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_reservation_qr)
        findViewById<View>(R.id.btnRefreshQr).setOnClickListener { load() }
    }

    override fun onResume() { super.onResume(); load() }
    override fun onPause() {
        findViewById<ImageView>(R.id.imgReservationQr).setImageDrawable(null)
        super.onPause()
    }

    private fun load() {
        val session = SessionManager(applicationContext)
        if (!session.isLoggedIn() || session.getUserRole() != "PROSUMER") { login(); return }
        val id = intent.getStringExtra("reservationId") ?: run { finish(); return }
        if (loading) return
        loading = true
        findViewById<View>(R.id.qrProgress).visibility = View.VISIBLE
        findViewById<ImageView>(R.id.imgReservationQr).setImageDrawable(null)
        lifecycleScope.launch {
            OperatorRepository().generateQr(id).onSuccess { qr ->
                val bitmap = withContext(Dispatchers.Default) {
                    runCatching { BarcodeEncoder().encodeBitmap(qr.qrToken, BarcodeFormat.QR_CODE, 720, 720) }.getOrNull()
                }
                if (bitmap == null) {
                    findViewById<TextView>(R.id.tvQrMessage).setText(R.string.operator_qr_render_failed)
                    return@onSuccess
                }
                findViewById<ImageView>(R.id.imgReservationQr).setImageBitmap(bitmap)
                findViewById<TextView>(R.id.tvQrMessage).text = getString(
                    R.string.operator_present_qr, qr.reservationId, ReservationFormat.format(qr.expiresAt),
                )
            }.onFailure { error ->
                findViewById<TextView>(R.id.tvQrMessage).text = error.message
                if ((error as? OperatorApiException)?.status == 401) { session.logout(); login() }
            }
            loading = false
            findViewById<View>(R.id.qrProgress).visibility = View.GONE
        }
    }

    private fun login() {
        startActivity(Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
