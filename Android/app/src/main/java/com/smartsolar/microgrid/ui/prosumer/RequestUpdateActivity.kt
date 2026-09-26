package com.smartsolar.microgrid.ui.prosumer

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.BookingSlotRepository
import com.smartsolar.microgrid.data.repository.ReservationRepository
import com.smartsolar.microgrid.data.repository.StationRepository
import com.smartsolar.microgrid.model.BookingSlot
import com.smartsolar.microgrid.model.UpdateReservationRequest
import com.smartsolar.microgrid.ui.adapter.BookingSlotsAdapter
import kotlinx.coroutines.launch

/**
 * Focused form for filing a change request against an existing
 * confirmed reservation. Picks a new slot, enters a new energy
 * amount, and POSTs the request.
 */
class RequestUpdateActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESERVATION_ID = "extra_reservation_id"
    }

    private val slotRepo = BookingSlotRepository()
    private val reservationRepo = ReservationRepository()
    private val stationRepo = StationRepository()

    private lateinit var reservationId: String
    private lateinit var rv: RecyclerView
    private lateinit var adapter: BookingSlotsAdapter
    private lateinit var tvNoSlots: TextView
    private lateinit var tvMessage: TextView
    private lateinit var etEnergy: TextInputEditText
    private lateinit var btnSubmit: MaterialButton

    private var selected: BookingSlot? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_update)

        val id = intent.getStringExtra(EXTRA_RESERVATION_ID)
        if (id.isNullOrBlank()) {
            finish()
            return
        }
        reservationId = id

        rv = findViewById(R.id.rvSlots)
        tvNoSlots = findViewById(R.id.tvNoSlots)
        tvMessage = findViewById(R.id.tvMessage)
        etEnergy = findViewById(R.id.etEnergy)
        btnSubmit = findViewById(R.id.btnSubmit)

        // The Request Update flow only needs the slot picker,
        // not an inline booking form — so we keep the details
        // panel visually collapsed and treat the button inside it
        // as the submit trigger by ignoring onBook here.
        adapter = BookingSlotsAdapter(
            onSelect = { slot -> selected = slot },
            onBook = { slot, _ -> selected = slot },
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        btnSubmit.setOnClickListener { submit() }

        load()
    }

    private fun load() {
        lifecycleScope.launch {
            stationRepo.getStations().onSuccess {
                adapter.setStations(it)
            }

            slotRepo.getSlots().onSuccess { data ->
                val active = data.filter {
                    it.isActive && it.availableCapacity > 0
                }
                tvNoSlots.visibility =
                    if (active.isEmpty()) View.VISIBLE else View.GONE
                adapter.submit(active, selected?.slotId)
            }.onFailure { throwable ->
                message(throwable.message ?: "Failed to load slots.", true)
            }
        }
    }

    private fun submit() {
        val slot = selected ?: run {
            message("Please pick a new slot.", true)
            return
        }

        val kwh = etEnergy.text?.toString()?.toDoubleOrNull()
        if (kwh == null || kwh <= 0) {
            message("Energy must be greater than zero.", true)
            return
        }
        if (kwh > slot.availableCapacity) {
            message(
                "Energy cannot exceed available capacity of ${slot.availableCapacity}.",
                true,
            )
            return
        }

        btnSubmit.isEnabled = false
        lifecycleScope.launch {
            val result = reservationRepo.requestUpdate(
                reservationId,
                UpdateReservationRequest(
                    slotId = slot.slotId,
                    scheduledStartTime = slot.startTime,
                    scheduledEndTime = slot.endTime,
                    energyAmountKwh = kwh,
                ),
            )
            btnSubmit.isEnabled = true

            result.onSuccess {
                message("Update request submitted for approval.", false)
                finish()
            }.onFailure { throwable ->
                message(throwable.message ?: "Update request failed.", true)
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
