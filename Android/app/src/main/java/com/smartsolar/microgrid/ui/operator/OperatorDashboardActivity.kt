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
import com.smartsolar.microgrid.ui.operator.OperatorReservationsActivity
import com.smartsolar.microgrid.ui.reservation.ReservationDetailsActivity
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class OperatorDashboardActivity : OperatorBaseActivity() {
    private val repo = OperatorRepository()
    private lateinit var adapter: ReservationsAdapter
    private lateinit var stationMap: MapView
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessionValid()) return
        setContentView(R.layout.activity_operator_dashboard)
        Configuration.getInstance().userAgentValue = packageName
        stationMap = findViewById<MapView>(R.id.operatorStationMap).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(7.0)
            controller.setCenter(GeoPoint(7.8731, 80.7718))
        }
        adapter = ReservationsAdapter { reservation ->
            startActivity(Intent(this, ReservationDetailsActivity::class.java)
                .putExtra(ReservationDetailsActivity.EXTRA_RESERVATION_JSON, Gson().toJson(reservation))
                .putExtra(ReservationDetailsActivity.EXTRA_VIEWER_ROLE, "GRID_OPERATOR"))
        }
        findViewById<RecyclerView>(R.id.rvJobs).apply {
            layoutManager = LinearLayoutManager(this@OperatorDashboardActivity)
            adapter = this@OperatorDashboardActivity.adapter
        }
        val openScanner = View.OnClickListener { startActivity(Intent(this, QrTransferActivity::class.java)) }
        findViewById<View>(R.id.btnScan).setOnClickListener(openScanner)
        findViewById<View>(R.id.btnBottomScan).setOnClickListener(openScanner)
        findViewById<View>(R.id.btnBottomMap).setOnClickListener {
            startActivity(Intent(this, OperatorMapActivity::class.java))
        }
        findViewById<View>(R.id.btnBottomHistory).setOnClickListener {
            startActivity(Intent(this, OperatorReservationsActivity::class.java))
        }
        findViewById<View>(R.id.btnRefresh).setOnClickListener { load() }
        findViewById<View>(R.id.btnOperatorLogout).setOnClickListener {
            startActivity(Intent(this, OperatorProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (::stationMap.isInitialized) stationMap.onResume()
        if (::adapter.isInitialized && !isFinishing) load()
    }

    override fun onPause() {
        if (::stationMap.isInitialized) stationMap.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        if (::stationMap.isInitialized) stationMap.onDetach()
        super.onDestroy()
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
                findViewById<TextView>(R.id.tvOperatorName).text = dashboard.operatorName
                findViewById<TextView>(R.id.tvTodayCount).text = getString(R.string.operator_today_count, dashboard.todayBookings)
                findViewById<TextView>(R.id.tvPendingCount).text = getString(R.string.operator_pending_count, dashboard.pendingBookings)
                findViewById<TextView>(R.id.tvCompletedCount).text = getString(R.string.operator_completed_count, dashboard.completedBookings)
                findViewById<TextView>(R.id.tvNearbyCount).text = dashboard.stations.size.toString()
                findViewById<TextView>(R.id.tvStationMapSummary).text = getString(R.string.operator_station_map_summary, dashboard.stations.size)
                renderStationsOnMap(dashboard.stations)
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

    private fun renderStationsOnMap(stations: List<com.smartsolar.microgrid.model.OperatorStation>) {
        val overlays = stationMap.overlays
        overlays.removeAll { it is Marker }
        val validStations = stations.filter { it.latitude != 0.0 || it.longitude != 0.0 }
        validStations.forEach { station ->
            overlays.add(Marker(stationMap).apply {
                position = GeoPoint(station.latitude, station.longitude)
                title = station.name
                snippet = station.stationId
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })
        }
        if (validStations.size == 1) {
            val station = validStations.first()
            stationMap.controller.setCenter(GeoPoint(station.latitude, station.longitude))
            stationMap.controller.setZoom(12.0)
        } else if (validStations.isNotEmpty()) {
            stationMap.controller.setCenter(GeoPoint(7.8731, 80.7718))
            stationMap.controller.setZoom(7.0)
        }
        stationMap.invalidate()
    }
}
