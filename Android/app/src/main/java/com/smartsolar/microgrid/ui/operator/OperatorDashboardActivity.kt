package com.smartsolar.microgrid.ui.operator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.OperatorRepository
import com.smartsolar.microgrid.ui.adapter.ReservationsAdapter
import com.smartsolar.microgrid.ui.reservation.ReservationDetailsActivity
import kotlinx.coroutines.launch

class OperatorDashboardActivity : OperatorBaseActivity() {
    private val repo = OperatorRepository()
    private lateinit var adapter: ReservationsAdapter
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessionValid()) return
        setContentView(R.layout.activity_operator_dashboard)
        adapter = ReservationsAdapter { reservation ->
            startActivity(Intent(this, ReservationDetailsActivity::class.java)
                .putExtra(ReservationDetailsActivity.EXTRA_RESERVATION_JSON, Gson().toJson(reservation))
                .putExtra(ReservationDetailsActivity.EXTRA_VIEWER_ROLE, "GRID_OPERATOR"))
        }
        findViewById<RecyclerView>(R.id.rvJobs).apply {
            layoutManager = LinearLayoutManager(this@OperatorDashboardActivity)
            adapter = this@OperatorDashboardActivity.adapter
        }
        findViewById<View>(R.id.btnScan).setOnClickListener { startActivity(Intent(this, QrTransferActivity::class.java)) }
        findViewById<View>(R.id.btnAvailability).setOnClickListener { startActivity(Intent(this, OperatorAvailabilityActivity::class.java)) }
        findViewById<View>(R.id.btnRefresh).setOnClickListener { load() }
        findViewById<View>(R.id.btnOperatorLogout).setOnClickListener { it.isEnabled = false; logout() }
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized && !isFinishing) load()
    }

    private fun load() {
        if (loading) return
        loading = true
        findViewById<View>(R.id.operatorProgress).visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repo.dashboard()
            loading = false
            findViewById<View>(R.id.operatorProgress).visibility = View.GONE
            result.onSuccess { dashboard ->
                findViewById<TextView>(R.id.tvOperatorName).text = getString(R.string.operator_greeting, dashboard.operatorName)
                findViewById<TextView>(R.id.tvTodayCount).text = getString(R.string.operator_today_count, dashboard.todayBookings)
                findViewById<TextView>(R.id.tvUpcomingCount).text = getString(R.string.operator_upcoming_count, dashboard.upcomingBookings)
                findViewById<TextView>(R.id.tvPendingCount).text = getString(R.string.operator_pending_count, dashboard.pendingBookings)
                findViewById<TextView>(R.id.tvCompletedCount).text = getString(R.string.operator_completed_count, dashboard.completedBookings)
                val jobs = dashboard.today + dashboard.upcoming
                adapter.submit(jobs)
                findViewById<TextView>(R.id.tvOperatorMessage).apply {
                    text = getString(if (dashboard.stations.isEmpty()) R.string.operator_no_stations else R.string.operator_no_jobs)
                    visibility = if (jobs.isEmpty()) View.VISIBLE else View.GONE
                }
            }.onFailure {
                findViewById<TextView>(R.id.tvOperatorMessage).apply { text = it.message; visibility = View.VISIBLE }
                showFailure(it)
            }
        }
    }
}
