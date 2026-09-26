package com.smartsolar.microgrid.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.model.Reservation
import com.smartsolar.microgrid.utils.ReservationFormat

/**
 * RecyclerView adapter for a list of reservations. Renders a
 * short summary row and forwards taps to the click callback so
 * the hosting activity can open the detail screen.
 */
class ReservationsAdapter(
    private val onClick: (Reservation) -> Unit,
) : RecyclerView.Adapter<ReservationsAdapter.VH>() {

    private val items = mutableListOf<Reservation>()

    fun submit(list: List<Reservation>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int,
    ): VH {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.item_reservation, parent, false)

        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvId = view.findViewById<TextView>(R.id.tvId)
        private val tvSchedule =
            view.findViewById<TextView>(R.id.tvSchedule)
        private val tvEnergy =
            view.findViewById<TextView>(R.id.tvEnergy)
        private val tvStatus =
            view.findViewById<TextView>(R.id.tvStatus)
        private val tvChange =
            view.findViewById<TextView>(R.id.tvChange)
        private val dotChange =
            view.findViewById<View>(R.id.dotChange)

        fun bind(reservation: Reservation) {
            tvId.text = reservation.reservationId

            tvSchedule.text = itemView.context.getString(
                R.string.reservation_schedule_range,
                ReservationFormat.format(reservation.scheduledStartTime),
                ReservationFormat.format(reservation.scheduledEndTime),
            )

            tvEnergy.text = itemView.context.getString(
                R.string.reservation_energy_kwh,
                reservation.energyAmountKwh,
            )

            val status = reservation.statusLabel()
            tvStatus.text = status
            tvStatus.setBackgroundResource(
                when (status) {
                    "PENDING" -> R.drawable.bg_badge_pending
                    "CONFIRMED", "COMPLETED" -> R.drawable.bg_badge_success
                    "CANCELLED" -> R.drawable.bg_badge_neutral
                    else -> R.drawable.bg_badge_neutral
                },
            )

            when {
                reservation.hasPendingChange -> {
                    tvChange.visibility = View.VISIBLE
                    tvChange.text = itemView.context.getString(
                        R.string.change_pending_label,
                    )
                    dotChange.visibility = View.VISIBLE
                }

                reservation.changeRequestStatus == "APPROVED" -> {
                    tvChange.visibility = View.VISIBLE
                    tvChange.text = itemView.context.getString(
                        R.string.change_approved_label,
                    )
                    dotChange.visibility = View.GONE
                }

                reservation.changeRequestStatus == "REJECTED" -> {
                    tvChange.visibility = View.VISIBLE
                    tvChange.text = itemView.context.getString(
                        R.string.change_rejected_label,
                    )
                    dotChange.visibility = View.GONE
                }

                else -> {
                    tvChange.visibility = View.GONE
                    dotChange.visibility = View.GONE
                }
            }

            itemView.setOnClickListener { onClick(reservation) }
        }
    }
}
