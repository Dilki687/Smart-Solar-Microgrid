package com.smartsolar.microgrid.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.model.BookingSlot
import com.smartsolar.microgrid.model.Station
import com.smartsolar.microgrid.utils.ReservationFormat

/**
 * RecyclerView adapter for the "Available Booking Slots" list.
 *
 * Tapping a slot expands an inline "Book this slot" panel
 * (energy input + submit button) directly beneath its row.
 * Tapping a different slot collapses the previous one and
 * expands the new one, so at most one panel is open at a time.
 *
 * The activity supplies:
 *   - [onSelect]  invoked whenever the currently expanded slot
 *                 changes (or clears);
 *   - [onBook]    invoked when the user submits the inline form,
 *                 with the currently entered energy text.
 * The activity does the validation + API call and calls
 * [showSlotMessage] to display any per-row error message.
 */
class BookingSlotsAdapter(
    private val onSelect: (BookingSlot?) -> Unit,
    private val onBook: (BookingSlot, String) -> Unit,
) : RecyclerView.Adapter<BookingSlotsAdapter.VH>() {

    private val items = mutableListOf<BookingSlot>()
    private var selectedSlotId: String? = null

    /**
     * Station lookup keyed by stationId, used to show the human
     * readable station name and kW capacity on each row.
     */
    private var stationLookup: Map<String, Station> = emptyMap()

    /** Update the station lookup without touching the slot list. */
    fun setStations(stations: List<Station>) {
        stationLookup = stations.associateBy { it.stationId }
        notifyDataSetChanged()
    }

    // We keep a live reference to the currently expanded holder
    // so the activity can push a per-row error into it.
    private var currentExpandedHolder: VH? = null

    fun submit(list: List<BookingSlot>, selected: String? = selectedSlotId) {
        items.clear()
        items.addAll(list)
        selectedSlotId = selected
        notifyDataSetChanged()
    }

    /** Currently expanded slot's id, or null when nothing is expanded. */
    fun selectedSlotId(): String? = selectedSlotId

    /**
     * Called by the activity to surface a per-row error message
     * (or clear it when null). Only affects the currently open row.
     */
    fun showSlotMessage(text: String?) {
        val holder = currentExpandedHolder ?: return
        holder.showMessage(text)
    }

    /** Called by the activity after a successful booking. */
    fun collapse() {
        val previous = selectedSlotId ?: return
        selectedSlotId = null
        val idx = items.indexOfFirst { it.slotId == previous }
        if (idx >= 0) notifyItemChanged(idx)
        currentExpandedHolder = null
        onSelect(null)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int,
    ): VH {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_booking_slot, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val summary = view.findViewById<View>(R.id.slotSummary)
        private val panel = view.findViewById<View>(R.id.slotPanel)

        private val tvStation = view.findViewById<TextView>(R.id.tvStation)
        private val tvStationSub =
            view.findViewById<TextView>(R.id.tvStationSub)
        private val tvStart = view.findViewById<TextView>(R.id.tvStart)
        private val tvEnd = view.findViewById<TextView>(R.id.tvEnd)
        private val tvCapacity = view.findViewById<TextView>(R.id.tvCapacity)
        private val etEnergy =
            view.findViewById<TextInputEditText>(R.id.etEnergy)
        private val btnBook = view.findViewById<MaterialButton>(R.id.btnBook)
        private val tvMessage =
            view.findViewById<TextView>(R.id.tvSlotMessage)

        fun bind(slot: BookingSlot) {
            val station = stationLookup[slot.stationId]

            // Show the human-readable station name when we know it,
            // fall back to the raw id otherwise.
            tvStation.text = station?.name ?: slot.stationId

            // Sub-line: station capacity in kW plus the id so
            // operators can still cross-reference if needed.
            tvStationSub.text = if (station != null) {
                itemView.context.getString(
                    R.string.slot_station_capacity,
                    station.capacityKw, station.stationId,
                )
            } else {
                slot.stationId
            }

            tvStart.text = ReservationFormat.format(slot.startTime)
            tvEnd.text = itemView.context.getString(
                R.string.slot_end_time,
                ReservationFormat.format(slot.endTime),
            )
            tvCapacity.text = itemView.context.getString(
                R.string.slot_available_of_total,
                slot.availableCapacity, slot.totalCapacity,
            )

            val isSelected = slot.slotId == selectedSlotId

            itemView.setBackgroundResource(
                if (isSelected) R.drawable.bg_slot_selected
                else R.drawable.bg_slot_unselected,
            )
            panel.visibility = if (isSelected) View.VISIBLE else View.GONE

            // Clear any per-row error and stale input each time we
            // freshly expand a row.
            if (isSelected) {
                tvMessage.visibility = View.GONE
                tvMessage.text = ""
                if (currentExpandedHolder !== this) {
                    etEnergy.setText("")
                }
                currentExpandedHolder = this
            } else if (currentExpandedHolder === this) {
                currentExpandedHolder = null
            }

            summary.setOnClickListener { toggle(slot) }

            btnBook.setOnClickListener {
                onBook(slot, etEnergy.text?.toString().orEmpty())
            }
        }

        fun showMessage(text: String?) {
            if (text.isNullOrBlank()) {
                tvMessage.visibility = View.GONE
                tvMessage.text = ""
            } else {
                tvMessage.text = text
                tvMessage.visibility = View.VISIBLE
            }
        }

        private fun toggle(slot: BookingSlot) {
            val parent = itemView.parent as? ViewGroup
            if (parent != null) {
                TransitionManager.beginDelayedTransition(
                    parent,
                    AutoTransition().apply { duration = 180 },
                )
            }

            val previous = selectedSlotId
            selectedSlotId =
                if (previous == slot.slotId) null else slot.slotId

            onSelect(items.firstOrNull { it.slotId == selectedSlotId })

            // Refresh the previously expanded row (if any) and this row.
            if (previous != null && previous != slot.slotId) {
                val idx = items.indexOfFirst { it.slotId == previous }
                if (idx >= 0) notifyItemChanged(idx)
            }
            notifyItemChanged(bindingAdapterPosition)
        }
    }
}
