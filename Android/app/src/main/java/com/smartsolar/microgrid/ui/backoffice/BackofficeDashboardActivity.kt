package com.smartsolar.microgrid.ui.backoffice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.BookingSlotRepository
import com.smartsolar.microgrid.data.repository.ProsumerRepository
import com.smartsolar.microgrid.data.repository.ReservationRepository
import com.smartsolar.microgrid.data.repository.StationRepository
import com.smartsolar.microgrid.data.repository.SystemUserRepository
import com.smartsolar.microgrid.model.Reservation
import com.smartsolar.microgrid.ui.adapter.ReservationsAdapter
import com.smartsolar.microgrid.ui.operator.OperatorReservationsActivity
import com.smartsolar.microgrid.ui.reservation.ReservationDetailsActivity
import com.smartsolar.microgrid.utils.SessionManager
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Backoffice landing screen. Same shape as the prosumer
 * dashboard: greeting + bell in the header, tinted stat tiles,
 * a preview of the items that need action (pending reservations),
 * and the quick-action cards below.
 */
class BackofficeDashboardActivity : BackofficeShellActivity() {

    override val contentLayoutRes = R.layout.activity_backoffice_dashboard
    override val currentSection = NavSection.DASHBOARD

    private val reservationRepo = ReservationRepository()
    private val prosumerRepo = ProsumerRepository()
    private val stationRepo = StationRepository()
    private val slotRepo = BookingSlotRepository()
    private val userRepo = SystemUserRepository()

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var tvGreeting: TextView
    private lateinit var notificationDot: View
    private lateinit var rvPending: RecyclerView
    private lateinit var tvPendingEmpty: TextView
    private lateinit var adapter: ReservationsAdapter

    private var pendingReservationsCount = 0
    private var pendingDeactivationsCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        swipe = findViewById(R.id.swipeRefresh)
        tvGreeting = findViewById(R.id.tvGreeting)
        notificationDot = findViewById(R.id.notificationDot)
        rvPending = findViewById(R.id.rvPending)
        tvPendingEmpty = findViewById(R.id.tvPendingEmpty)

        adapter = ReservationsAdapter { openReservation(it) }
        rvPending.layoutManager = LinearLayoutManager(this)
        rvPending.adapter = adapter

        setUpTile(
            R.id.statPendingReservations,
            R.string.stat_pending_reservations, R.drawable.bg_stat_amber,
        )
        setUpTile(
            R.id.statDeactivations,
            R.string.stat_deactivation_requests, R.drawable.bg_stat_rose,
        )
        setUpTile(
            R.id.statStations,
            R.string.stat_active_stations, R.drawable.bg_stat_green,
        )
        setUpTile(
            R.id.statSlots,
            R.string.stat_active_slots, R.drawable.bg_stat_violet,
        )
        setUpTile(
            R.id.statUsers,
            R.string.stat_system_users, R.drawable.bg_stat_blue,
        )

        // Quick-action cards -- unchanged behaviour, just re-wired.
        setUpCard(R.id.cardUsers, R.string.qa_manage_users, R.string.qa_manage_users_sub) {
            open(UserManagementActivity::class.java)
        }
        setUpCard(R.id.cardProsumers, R.string.qa_manage_prosumers, R.string.qa_manage_prosumers_sub) {
            open(ProsumerLookupActivity::class.java)
        }
        setUpCard(R.id.cardDeactivation, R.string.qa_deactivation, R.string.qa_deactivation_sub) {
            open(DeactivationRequestsActivity::class.java)
        }
        setUpCard(R.id.cardStations, R.string.qa_stations, R.string.qa_stations_sub) {
            open(StationManagementActivity::class.java)
        }
        setUpCard(R.id.cardSlots, R.string.qa_slots, R.string.qa_slots_sub) {
            open(BookingSlotManagementActivity::class.java)
        }
        setUpCard(R.id.cardReservations, R.string.qa_reservations, R.string.qa_reservations_sub) {
            open(OperatorReservationsActivity::class.java)
        }

        findViewById<MaterialButton>(R.id.btnAllReservations).setOnClickListener {
            open(OperatorReservationsActivity::class.java)
        }

        findViewById<ImageButton>(R.id.btnNotifications).setOnClickListener {
            val bits = mutableListOf<String>()
            if (pendingReservationsCount > 0) {
                bits += "$pendingReservationsCount reservation" +
                        (if (pendingReservationsCount == 1) "" else "s") +
                        " waiting for approval"
            }
            if (pendingDeactivationsCount > 0) {
                bits += "$pendingDeactivationsCount deactivation request" +
                        (if (pendingDeactivationsCount == 1) "" else "s")
            }
            val text =
                if (bits.isEmpty()) "No new notifications."
                else bits.joinToString(". ") + "."
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }

        swipe.setOnRefreshListener { load() }
        renderGreeting()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun renderGreeting() {
        val name = SessionManager(applicationContext).getCurrentUser()?.name
            ?.substringBefore(' ')
            ?: "there"
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val template = when {
            hour < 12 -> R.string.greeting_morning
            hour < 17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
        tvGreeting.text = getString(template, name)
    }

    private fun setUpTile(tileId: Int, labelRes: Int, bgRes: Int) {
        val tile = findViewById<View>(tileId)
        tile.setBackgroundResource(bgRes)
        tile.findViewById<TextView>(R.id.tvStatLabel).setText(labelRes)
        tile.findViewById<TextView>(R.id.tvStatValue).text = "—"
    }

    private fun setTileValue(tileId: Int, value: Int) {
        findViewById<View>(tileId)
            .findViewById<TextView>(R.id.tvStatValue)
            .text = value.toString()
    }

    private fun setUpCard(
        rootId: Int,
        @StringRes titleRes: Int,
        @StringRes subRes: Int,
        onClick: () -> Unit,
    ) {
        val root = findViewById<View>(rootId)
        root.findViewById<TextView>(R.id.tvCardTitle).setText(titleRes)
        root.findViewById<TextView>(R.id.tvCardSub).setText(subRes)
        root.setOnClickListener { onClick() }
    }

    private fun open(target: Class<*>) {
        startActivity(Intent(this, target))
    }

    // ---------- data ----------

    private fun load() {
        swipe.isRefreshing = true

        lifecycleScope.launch {
            // Fire every request in parallel; failures fall back
            // to sensible defaults so a single broken endpoint
            // doesn't blank the whole dashboard.
            val reservationsD =
                async { reservationRepo.getAllReservations() }
            val deactivationsD =
                async { prosumerRepo.deactivationRequests() }
            val stationsD = async { stationRepo.getStations() }
            val slotsD = async { slotRepo.getSlots() }
            val usersD = async { userRepo.list() }

            val reservations = reservationsD.await().getOrDefault(emptyList())
            val deactivations = deactivationsD.await().getOrDefault(emptyList())
            val stations = stationsD.await().getOrDefault(emptyList())
            val slots = slotsD.await().getOrDefault(emptyList())
            val users = usersD.await().getOrDefault(emptyList())

            swipe.isRefreshing = false

            pendingReservationsCount = reservations.count {
                it.statusLabel() == "PENDING"
            }
            pendingDeactivationsCount = deactivations.size

            setTileValue(R.id.statPendingReservations, pendingReservationsCount)
            setTileValue(R.id.statDeactivations, pendingDeactivationsCount)
            setTileValue(
                R.id.statStations,
                stations.count { it.status == "ACTIVE" || it.status == null },
            )
            setTileValue(R.id.statSlots, slots.count { it.isActive })
            setTileValue(R.id.statUsers, users.size)

            val pending = reservations
                .filter { it.statusLabel() == "PENDING" }
                .take(5)
            adapter.submit(pending)
            tvPendingEmpty.visibility =
                if (pending.isEmpty()) View.VISIBLE else View.GONE

            notificationDot.visibility =
                if (pendingReservationsCount + pendingDeactivationsCount > 0)
                    View.VISIBLE
                else View.GONE
        }
    }

    private fun openReservation(reservation: Reservation) {
        val intent = Intent(this, ReservationDetailsActivity::class.java)
        intent.putExtra(
            ReservationDetailsActivity.EXTRA_RESERVATION_JSON,
            com.google.gson.Gson().toJson(reservation),
        )
        intent.putExtra(
            ReservationDetailsActivity.EXTRA_VIEWER_ROLE,
            SessionManager(applicationContext).getUserRole()
                ?: "BACKOFFICE",
        )
        startActivity(intent)
    }
}
