package com.smartsolar.microgrid.ui.prosumer

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.BookingSlotRepository
import com.smartsolar.microgrid.data.repository.ReservationRepository
import com.smartsolar.microgrid.data.repository.StationRepository
import com.smartsolar.microgrid.model.BookingSlot
import com.smartsolar.microgrid.model.CreateReservationRequest
import com.smartsolar.microgrid.ui.adapter.BookingSlotsAdapter
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Booking screen.
 *
 * Shows every active slot with spare capacity. Tapping a slot
 * expands an inline "Book this slot" panel beneath it; the
 * activity performs validation and submits the reservation via
 * [ReservationRepository].
 */
class BookingActivity : ProsumerShellActivity() {

    override val contentLayoutRes: Int = R.layout.activity_booking
    override val currentSection: NavSection = NavSection.BOOKING

    private val slotRepo = BookingSlotRepository()
    private val reservationRepo = ReservationRepository()
    private val stationRepo = StationRepository()

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rv: RecyclerView
    private lateinit var adapter: BookingSlotsAdapter

    private lateinit var tvNoSlots: TextView
    private lateinit var tvMessage: TextView

    private var slots: List<BookingSlot> = emptyList()
    private var isSubmitting: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        swipe = findViewById(R.id.swipeRefresh)
        rv = findViewById(R.id.rvSlots)
        tvNoSlots = findViewById(R.id.tvNoSlots)
        tvMessage = findViewById(R.id.tvMessage)

        adapter = BookingSlotsAdapter(
            onSelect = { /* activity doesn't need per-select action */ },
            onBook = { slot, kwhInput -> submit(slot, kwhInput) },
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        swipe.setOnRefreshListener { load() }
        load()
    }

    private fun load() {
        swipe.isRefreshing = true
        message(null, isError = false)

        lifecycleScope.launch {
            // Fetch slots and stations in parallel — stations are
            // small and mostly used for pretty-printing the row.
            val slotsDeferred = async { slotRepo.getSlots() }
            val stationsDeferred = async { stationRepo.getStations() }

            val slotResult = slotsDeferred.await()
            val stationResult = stationsDeferred.await()
            swipe.isRefreshing = false

            stationResult.onSuccess { adapter.setStations(it) }

            slotResult.onSuccess { data ->
                slots = data.filter { it.isActive && it.availableCapacity > 0 }
                tvNoSlots.visibility =
                    if (slots.isEmpty()) View.VISIBLE else View.GONE
                adapter.submit(slots)
            }.onFailure { throwable ->
                message(throwable.message ?: "Failed to load slots.", true)
            }
        }
    }

    private fun submit(slot: BookingSlot, kwhInput: String) {
        if (isSubmitting) return

        val kwh = kwhInput.toDoubleOrNull()
        if (kwh == null || kwh <= 0) {
            adapter.showSlotMessage("Energy must be greater than zero.")
            return
        }
        if (kwh > slot.availableCapacity) {
            adapter.showSlotMessage(
                "Energy cannot exceed available capacity of ${slot.availableCapacity}.",
            )
            return
        }

        isSubmitting = true
        adapter.showSlotMessage(null)

        lifecycleScope.launch {
            val result = reservationRepo.createReservation(
                CreateReservationRequest(
                    slotId = slot.slotId,
                    stationId = slot.stationId,
                    scheduledStartTime = slot.startTime,
                    scheduledEndTime = slot.endTime,
                    energyAmountKwh = kwh,
                ),
            )
            isSubmitting = false

            result.onSuccess {
                adapter.collapse()
                message("Reservation created and awaiting approval.", false)
                load()
            }.onFailure { throwable ->
                adapter.showSlotMessage(
                    throwable.message ?: "Could not create reservation.",
                )
            }
        }
    }

    private fun message(text: String?, isError: Boolean) {
        if (text.isNullOrBlank()) {
            tvMessage.visibility = View.GONE
            return
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
}
