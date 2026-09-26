package com.smartsolar.microgrid.ui.reservation

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.ReservationRepository
import com.smartsolar.microgrid.model.Reservation
import com.smartsolar.microgrid.ui.prosumer.RequestUpdateActivity
import com.smartsolar.microgrid.utils.ReservationFormat
import kotlinx.coroutines.launch

/**
 * Shared reservation detail screen used by both prosumer and
 * officer flows. Which action buttons appear depends on the
 * viewer's role and on the reservation's lifecycle state.
 */
class ReservationDetailsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESERVATION_JSON = "extra_reservation_json"
        const val EXTRA_VIEWER_ROLE = "extra_viewer_role"
    }

    private val repo = ReservationRepository()
    private val gson = Gson()

    private lateinit var reservation: Reservation
    private lateinit var viewerRole: String

    private lateinit var tvMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_details)

        val json = intent.getStringExtra(EXTRA_RESERVATION_JSON)
        val role = intent.getStringExtra(EXTRA_VIEWER_ROLE)
        if (json.isNullOrBlank() || role.isNullOrBlank()) {
            finish()
            return
        }
        reservation = gson.fromJson(json, Reservation::class.java)
        viewerRole = role

        tvMessage = findViewById(R.id.tvMessage)

        render()
    }

    private fun render() {
        findViewById<TextView>(R.id.tvHeaderId).text = reservation.reservationId

        setRow(R.id.rowStatus, R.string.label_status, reservation.statusLabel())
        setRow(
            R.id.rowChange, R.string.label_change_request,
            reservation.changeRequestStatus?.takeIf { it.isNotBlank() } ?: "None",
        )
        setRow(R.id.rowProsumer, R.string.label_prosumer, reservation.prosumerUserId)
        setRow(R.id.rowStation, R.string.label_station, reservation.stationId)
        setRow(R.id.rowSlot, R.string.label_slot, reservation.slotId)
        setRow(
            R.id.rowStart, R.string.label_scheduled_start,
            ReservationFormat.format(reservation.scheduledStartTime),
        )
        setRow(
            R.id.rowEnd, R.string.label_scheduled_end,
            ReservationFormat.format(reservation.scheduledEndTime),
        )
        setRow(
            R.id.rowEnergy, R.string.label_energy,
            String.format("%.2f", reservation.energyAmountKwh),
        )

        val cancelReason = reservation.cancellationReason
        val cancelRow = findViewById<View>(R.id.rowCancelReason)
        if (!cancelReason.isNullOrBlank()) {
            setRow(R.id.rowCancelReason, R.string.label_cancellation_reason, cancelReason)
            cancelRow.visibility = View.VISIBLE
        } else {
            cancelRow.visibility = View.GONE
        }

        renderPendingChange()
        renderActions()
    }

    private fun renderPendingChange() {
        val card = findViewById<View>(R.id.pendingCard)
        if (!reservation.hasPendingChange) {
            card.visibility = View.GONE
            return
        }
        card.visibility = View.VISIBLE

        setRow(
            R.id.rowPendingSlot, R.string.pending_slot,
            reservation.pendingSlotId ?: "—",
        )
        setRow(
            R.id.rowPendingStart, R.string.pending_start,
            ReservationFormat.format(reservation.pendingScheduledStartTime),
        )
        setRow(
            R.id.rowPendingEnd, R.string.pending_end,
            ReservationFormat.format(reservation.pendingScheduledEndTime),
        )
        setRow(
            R.id.rowPendingEnergy, R.string.pending_energy,
            reservation.pendingEnergyAmountKwh
                ?.let { String.format("%.2f", it) } ?: "—",
        )
    }

    private fun renderActions() {
        val status = reservation.statusLabel()
        val hasChange = reservation.hasPendingChange
        findViewById<View>(R.id.btnShowQr).apply {
            visibility = if (viewerRole == "PROSUMER" && status == "CONFIRMED" && !hasChange) View.VISIBLE else View.GONE
            setOnClickListener {
                startActivity(Intent(this@ReservationDetailsActivity,
                    com.smartsolar.microgrid.ui.prosumer.ReservationQrActivity::class.java)
                    .putExtra("reservationId", reservation.reservationId))
            }
        }

        val btnApprove = findViewById<MaterialButton>(R.id.btnApprove)
        val btnReject = findViewById<MaterialButton>(R.id.btnReject)
        val btnApproveChange = findViewById<MaterialButton>(R.id.btnApproveChange)
        val btnRejectChange = findViewById<MaterialButton>(R.id.btnRejectChange)
        val btnRequestUpdate = findViewById<MaterialButton>(R.id.btnRequestUpdate)
        val btnCancel = findViewById<MaterialButton>(R.id.btnCancel)

        // Reset every button.
        for (b in arrayOf(btnApprove, btnReject, btnApproveChange,
                          btnRejectChange, btnRequestUpdate, btnCancel)) {
            b.visibility = View.GONE
        }

        val isOfficer =
            viewerRole == "BACKOFFICE" || viewerRole == "GRID_OPERATOR"

        if (isOfficer) {
            if (status == "PENDING") {
                btnApprove.visibility = View.VISIBLE
                btnReject.visibility = View.VISIBLE
                btnApprove.setOnClickListener { officerApprove() }
                btnReject.setOnClickListener { officerReject() }
            }
            if (hasChange && (status == "PENDING" || status == "CONFIRMED")) {
                btnApproveChange.visibility = View.VISIBLE
                btnRejectChange.visibility = View.VISIBLE
                btnApproveChange.setOnClickListener { officerApproveChange() }
                btnRejectChange.setOnClickListener { officerRejectChange() }
            }
        } else if (viewerRole == "PROSUMER") {
            if (status == "CONFIRMED") {
                btnCancel.visibility = View.VISIBLE
                btnCancel.setOnClickListener { prosumerCancel() }

                if (!hasChange) {
                    btnRequestUpdate.visibility = View.VISIBLE
                    btnRequestUpdate.setOnClickListener { openUpdateForm() }
                }
            }
        }
    }

    private fun setRow(id: Int, labelRes: Int, value: String) {
        val row = findViewById<View>(id)
        row.findViewById<TextView>(R.id.tvLabel).setText(labelRes)
        row.findViewById<TextView>(R.id.tvValue).text = value
    }

    // ---------- action helpers ----------

    private fun officerApprove() {
        confirm("Approve this reservation?") {
            perform {
                repo.approve(reservation.reservationId)
                    .map { "Reservation approved." }
            }
        }
    }

    private fun officerReject() {
        askReason("Enter the rejection reason:") { reason ->
            perform {
                repo.reject(reservation.reservationId, reason)
                    .map { "Reservation rejected." }
            }
        }
    }

    private fun officerApproveChange() {
        confirm("Approve the pending change?") {
            perform {
                repo.approveChange(reservation.reservationId)
                    .map { "Change request approved." }
            }
        }
    }

    private fun officerRejectChange() {
        askReason("Enter the reason for rejecting the change:") { reason ->
            perform {
                repo.rejectChange(reservation.reservationId, reason)
                    .map { "Change request rejected." }
            }
        }
    }

    private fun prosumerCancel() {
        askReason("Reason for cancelling:") { reason ->
            confirm("Are you sure you want to cancel this reservation?") {
                perform {
                    repo.cancel(reservation.reservationId, reason)
                        .map { "Reservation cancelled." }
                }
            }
        }
    }

    private fun openUpdateForm() {
        val intent = Intent(this, RequestUpdateActivity::class.java)
        intent.putExtra(
            RequestUpdateActivity.EXTRA_RESERVATION_ID,
            reservation.reservationId,
        )
        startActivity(intent)
        finish()
    }

    // ---------- shared UI helpers ----------

    private fun confirm(question: String, onYes: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(question)
            .setPositiveButton("Yes") { _, _ -> onYes() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun askReason(prompt: String, onReason: (String) -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine(false)
        }
        AlertDialog.Builder(this)
            .setTitle(prompt)
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isEmpty()) {
                    message("A reason is required.", true)
                } else {
                    onReason(reason)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun perform(block: suspend () -> Result<String>) {
        lifecycleScope.launch {
            val result = block()
            result.onSuccess {
                message(it, false)
                // Simple sync: return to list which will refetch onResume.
                finish()
            }.onFailure { throwable ->
                message(throwable.message ?: "Action failed.", true)
            }
        }
    }

    private fun message(text: String, isError: Boolean) {
        tvMessage.text = text
        tvMessage.setBackgroundResource(
            if (isError) R.color.tone_rose_bg else R.color.tone_green_bg,
        )
        tvMessage.setTextColor(
            resources.getColor(
                if (isError) R.color.tone_rose_fg else R.color.tone_green_fg,
                theme,
            ),
        )
        tvMessage.visibility = View.VISIBLE
    }
}
