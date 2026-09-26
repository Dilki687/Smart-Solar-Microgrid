package com.smartsolar.microgrid.ui.prosumer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.ReservationRepository
import com.smartsolar.microgrid.model.Reservation
import com.smartsolar.microgrid.ui.adapter.ReservationsAdapter
import com.smartsolar.microgrid.ui.reservation.ReservationDetailsActivity
import com.smartsolar.microgrid.utils.ReservationFormat
import com.smartsolar.microgrid.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Prosumer landing screen. Lives inside [ProsumerShellActivity]
 * which supplies the toolbar hamburger and the sliding navigation
 * drawer.
 */
class ProsumerDashboardActivity : ProsumerShellActivity() {

    override val contentLayoutRes: Int = R.layout.activity_prosumer_dashboard
    override val currentSection: NavSection = NavSection.DASHBOARD

    private val reservationRepo = ReservationRepository()

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rvUpcoming: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvGreeting: TextView
    private lateinit var notificationDot: View
    private lateinit var adapter: ReservationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        swipe = findViewById(R.id.swipeRefresh)
        rvUpcoming = findViewById(R.id.rvUpcoming)
        tvEmpty = findViewById(R.id.tvUpcomingEmpty)
        tvGreeting = findViewById(R.id.tvGreeting)
        notificationDot = findViewById(R.id.notificationDot)

        adapter = ReservationsAdapter { open(it) }
        rvUpcoming.layoutManager = LinearLayoutManager(this)
        rvUpcoming.adapter = adapter

        setUpTile(R.id.statTotal, R.string.stat_total, R.drawable.bg_stat_blue)
        setUpTile(R.id.statPending, R.string.stat_pending, R.drawable.bg_stat_amber)
        setUpTile(R.id.statUpcoming, R.string.stat_upcoming, R.drawable.bg_stat_violet)
        setUpTile(R.id.statCompleted, R.string.stat_completed, R.drawable.bg_stat_green)
        setUpTile(R.id.statCancelled, R.string.stat_cancelled, R.drawable.bg_stat_rose)

        findViewById<MaterialButton>(R.id.btnAllReservations).setOnClickListener {
            startActivity(Intent(this, MyReservationsActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnNotifications).setOnClickListener {
            // Placeholder — tapping the bell just re-syncs for now.
            // A real notification centre is future work.
            Toast.makeText(
                this,
                if (notificationDot.visibility == View.VISIBLE)
                    "You have updates on one or more reservations."
                else "No new notifications.",
                Toast.LENGTH_SHORT,
            ).show()
        }

        swipe.setOnRefreshListener { load() }

        renderGreeting()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun renderGreeting() {
        val name =
            SessionManager(applicationContext).getCurrentUser()?.name
                ?.substringBefore(' ')
                ?: "there"
        val hour =
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
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
    }

    private fun setTileValue(tileId: Int, value: Int) {
        findViewById<View>(tileId)
            .findViewById<TextView>(R.id.tvStatValue)
            .text = value.toString()
    }

    private fun load() {
        swipe.isRefreshing = true

        lifecycleScope.launch {
            val result = reservationRepo.getMyReservations()
            swipe.isRefreshing = false

            result.onSuccess { list ->
                render(list)
            }.onFailure { throwable ->
                render(emptyList())
                tvEmpty.text = throwable.message
                    ?: getString(R.string.my_reservations_empty)
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun render(reservations: List<Reservation>) {
        val now = System.currentTimeMillis()

        var pending = 0
        var upcoming = 0
        var completed = 0
        var cancelled = 0

        var pendingChangeCount = 0

        val upcomingList = mutableListOf<Reservation>()

        for (r in reservations) {
            if (r.hasPendingChange) pendingChangeCount++

            when (r.statusLabel()) {
                "PENDING" -> pending++
                "CONFIRMED" -> {
                    val start = ReservationFormat.parse(r.scheduledStartTime)?.time
                    if (start != null && start >= now) {
                        upcoming++
                        upcomingList.add(r)
                    }
                }
                "COMPLETED" -> completed++
                "CANCELLED" -> cancelled++
            }
        }

        setTileValue(R.id.statTotal, reservations.size)
        setTileValue(R.id.statPending, pending)
        setTileValue(R.id.statUpcoming, upcoming)
        setTileValue(R.id.statCompleted, completed)
        setTileValue(R.id.statCancelled, cancelled)

        upcomingList.sortBy {
            ReservationFormat.parse(it.scheduledStartTime)?.time ?: Long.MAX_VALUE
        }
        adapter.submit(upcomingList.take(5))

        tvEmpty.visibility =
            if (upcomingList.isEmpty()) View.VISIBLE else View.GONE
        if (upcomingList.isEmpty()) {
            tvEmpty.text = getString(R.string.upcoming_empty)
        }

        notificationDot.visibility =
            if (pendingChangeCount > 0) View.VISIBLE else View.GONE
    }

    private fun open(reservation: Reservation) {
        val intent = Intent(this, ReservationDetailsActivity::class.java)
        intent.putExtra(
            ReservationDetailsActivity.EXTRA_RESERVATION_JSON,
            com.google.gson.Gson().toJson(reservation),
        )
        intent.putExtra(
            ReservationDetailsActivity.EXTRA_VIEWER_ROLE, "PROSUMER",
        )
        startActivity(intent)
    }
}
