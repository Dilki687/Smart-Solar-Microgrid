package com.smartsolar.microgrid.ui.backoffice

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.ProsumerRepository
import com.smartsolar.microgrid.model.Prosumer
import kotlinx.coroutines.launch

/**
 * The backend doesn't have a "list all prosumers" endpoint, so
 * this screen mirrors the web behaviour: type a NIC, look it up,
 * then deactivate / reactivate from the result card.
 */
class ProsumerLookupActivity : BackofficeShellActivity() {

    override val contentLayoutRes = R.layout.activity_prosumer_lookup
    override val currentSection = NavSection.PROSUMERS

    private val repo = ProsumerRepository()

    private lateinit var etNic: TextInputEditText
    private lateinit var btnLookup: MaterialButton
    private lateinit var tvMessage: TextView
    private lateinit var resultCard: View
    private lateinit var tvName: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnDeactivate: MaterialButton
    private lateinit var btnReactivate: MaterialButton

    private var current: Prosumer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        etNic = findViewById(R.id.etNic)
        btnLookup = findViewById(R.id.btnLookup)
        tvMessage = findViewById(R.id.tvMessage)
        resultCard = findViewById(R.id.resultCard)
        tvName = findViewById(R.id.tvName)
        tvStatus = findViewById(R.id.tvStatus)
        btnDeactivate = findViewById(R.id.btnDeactivate)
        btnReactivate = findViewById(R.id.btnReactivate)

        setRow(R.id.rowNic, R.string.label_reservation_id)
        (findViewById<View>(R.id.rowNic).findViewById<TextView>(R.id.tvLabel))
            .setText("NIC")
        (findViewById<View>(R.id.rowEmail).findViewById<TextView>(R.id.tvLabel))
            .setText("Email")
        (findViewById<View>(R.id.rowPhone).findViewById<TextView>(R.id.tvLabel))
            .setText("Phone")
        (findViewById<View>(R.id.rowAddress).findViewById<TextView>(R.id.tvLabel))
            .setText("Address")

        btnLookup.setOnClickListener { lookup() }

        btnDeactivate.setOnClickListener {
            current?.let { p ->
                confirm("Deactivate ${p.name}?") {
                    lifecycleScope.launch {
                        repo.deactivate(p.nic).fold(
                            onSuccess = { lookup() },
                            onFailure = { showMessage(it.message, true) },
                        )
                    }
                }
            }
        }
        btnReactivate.setOnClickListener {
            current?.let { p ->
                confirm("Reactivate ${p.name}?") {
                    lifecycleScope.launch {
                        repo.reactivate(p.nic).fold(
                            onSuccess = { lookup() },
                            onFailure = { showMessage(it.message, true) },
                        )
                    }
                }
            }
        }
    }

    private fun setRow(id: Int, @StringRes labelRes: Int) {
        findViewById<View>(id)
            .findViewById<TextView>(R.id.tvLabel).setText(labelRes)
    }

    private fun setValue(id: Int, value: String?) {
        findViewById<View>(id)
            .findViewById<TextView>(R.id.tvValue).text = value ?: "—"
    }

    private fun lookup() {
        val nic = etNic.text?.toString()?.trim().orEmpty()
        if (nic.isEmpty()) {
            showMessage("Enter a NIC.", true); return
        }

        showMessage(null, false)
        btnLookup.isEnabled = false
        lifecycleScope.launch {
            val result = repo.get(nic)
            btnLookup.isEnabled = true
            result.fold(
                onSuccess = { render(it) },
                onFailure = {
                    resultCard.visibility = View.GONE
                    showMessage(it.message, true)
                },
            )
        }
    }

    private fun render(p: Prosumer) {
        current = p
        resultCard.visibility = View.VISIBLE
        tvName.text = p.name
        tvStatus.text = p.status
        tvStatus.setBackgroundResource(
            when (p.status) {
                "ACTIVE" -> R.drawable.bg_badge_success
                "PENDING_DEACTIVATION" -> R.drawable.bg_badge_pending
                else -> R.drawable.bg_badge_neutral
            },
        )
        setValue(R.id.rowNic, p.nic)
        setValue(R.id.rowEmail, p.email)
        setValue(R.id.rowPhone, p.phone)
        setValue(R.id.rowAddress, p.address)

        val isActive = p.status == "ACTIVE" ||
                p.status == "PENDING_DEACTIVATION"
        btnDeactivate.isEnabled = isActive
        btnReactivate.isEnabled = p.status != "ACTIVE"
    }

    private fun showMessage(text: String?, isError: Boolean) {
        if (text.isNullOrBlank()) {
            tvMessage.visibility = View.GONE; return
        }
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

    private fun confirm(msg: String, onYes: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton("Yes") { _, _ -> onYes() }
            .setNegativeButton("No", null)
            .show()
    }
}
