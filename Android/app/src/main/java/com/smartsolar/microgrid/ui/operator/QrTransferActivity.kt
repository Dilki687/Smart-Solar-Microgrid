package com.smartsolar.microgrid.ui.operator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.OperatorRepository
import com.smartsolar.microgrid.model.TransferDetails
import com.smartsolar.microgrid.utils.ReservationFormat
import kotlinx.coroutines.launch

class QrTransferActivity : OperatorBaseActivity() {
    private val repo = OperatorRepository()
    private var details: TransferDetails? = null
    private var token: String? = null
    private var busy = false

    private val scanner = registerForActivityResult(ScanContract()) { result ->
        val scanned = result.contents
        if (scanned == null) message(getString(R.string.operator_scan_cancelled))
        else { token = scanned; verify(scanned) }
    }
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else message(getString(R.string.operator_camera_denied))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessionValid()) return
        setContentView(R.layout.activity_qr_transfer)
        findViewById<View>(R.id.btnScanAgain).setOnClickListener { scan() }
        findViewById<View>(R.id.btnCompleteTransfer).setOnClickListener {
            if (!busy && details?.transactionStatus == "VERIFIED") {
                AlertDialog.Builder(this).setTitle(R.string.operator_complete)
                    .setMessage(R.string.operator_confirm_physical)
                    .setPositiveButton(R.string.operator_confirm) { _, _ -> complete() }
                    .setNegativeButton(android.R.string.cancel, null).show()
            }
        }
        findViewById<View>(R.id.btnTransferDashboard).setOnClickListener { finish() }
        val completed = savedInstanceState?.getString("completed")
        token = savedInstanceState?.getString("token")
        if (completed != null) {
            details = Gson().fromJson(completed, TransferDetails::class.java)
            render()
        } else if (token != null) {
            verify(token!!)
        } else if (savedInstanceState == null) {
            scan()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("token", token)
        if (details?.transactionStatus == "COMPLETED") outState.putString("completed", Gson().toJson(details))
        super.onSaveInstanceState(outState)
    }

    private fun scan() {
        if (busy) return
        details = null
        token = null
        findViewById<View>(R.id.btnCompleteTransfer).visibility = View.GONE
        findViewById<View>(R.id.btnScanAgain).visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvTransferDetails).text = ""
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            message(getString(R.string.operator_no_camera)); return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera()
        else permission.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt(getString(R.string.operator_scan_prompt)).setBeepEnabled(false)
            .setBarcodeImageEnabled(false).setOrientationLocked(false))
    }

    private fun verify(scanned: String) {
        details = null
        setBusy(true)
        message(getString(R.string.operator_verifying))
        lifecycleScope.launch {
            repo.verify(scanned).onSuccess { details = it; render() }
                .onFailure { message(it.message ?: getString(R.string.operator_invalid_qr)); showFailure(it) }
            setBusy(false)
        }
    }

    private fun complete() {
        val verified = details ?: return
        if (busy || verified.transactionStatus != "VERIFIED") return
        setBusy(true)
        lifecycleScope.launch {
            repo.complete(verified.transactionId).onSuccess {
                details = it
                token = null
                render()
            }.onFailure {
                // A lost response may mean the server already committed. Re-scan to resolve it.
                details = null
                findViewById<View>(R.id.btnCompleteTransfer).visibility = View.GONE
                message(it.message ?: getString(R.string.operator_invalid_qr))
                showFailure(it)
            }
            setBusy(false)
        }
    }

    private fun render() {
        val d = details ?: return
        val completed = d.transactionStatus == "COMPLETED"
        message(getString(if (completed) R.string.operator_transfer_completed else R.string.operator_qr_verified))
        findViewById<TextView>(R.id.tvTransferDetails).text = getString(
            R.string.operator_transfer_details, d.prosumerName.orEmpty(), d.prosumerNIC,
            d.reservationId, d.stationName ?: d.stationId, d.slotId,
            ReservationFormat.format(d.scheduledStartTime), ReservationFormat.format(d.scheduledEndTime),
            d.energyAmountKwh, d.reservationStatus, d.transactionId, d.transactionStatus,
            d.completedAt?.let { ReservationFormat.format(it) } ?: getString(R.string.operator_not_completed),
        )
        findViewById<View>(R.id.btnCompleteTransfer).visibility = if (completed) View.GONE else View.VISIBLE
        findViewById<View>(R.id.btnScanAgain).visibility = View.GONE
    }

    private fun setBusy(value: Boolean) {
        busy = value
        findViewById<View>(R.id.transferProgress).visibility = if (value) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnCompleteTransfer).isEnabled = !value && details?.transactionStatus == "VERIFIED"
        findViewById<View>(R.id.btnScanAgain).isEnabled = !value
    }

    private fun message(value: String) { findViewById<TextView>(R.id.tvTransferMessage).text = value }
}
