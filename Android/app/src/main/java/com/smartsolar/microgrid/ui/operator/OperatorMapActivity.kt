package com.smartsolar.microgrid.ui.operator

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.smartsolar.microgrid.R
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class OperatorMapActivity : OperatorBaseActivity() {
    private lateinit var map: MapView
    private val repository = com.smartsolar.microgrid.data.repository.OperatorRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessionValid()) return
        setContentView(R.layout.activity_operator_map)

        Configuration.getInstance().userAgentValue = packageName
        map = findViewById<MapView>(R.id.operatorFullMap).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(7.0)
            controller.setCenter(GeoPoint(7.8731, 80.7718))
        }

        findViewById<View>(R.id.btnMapBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnMapRetry).setOnClickListener { loadStations() }
        loadStations()
    }

    override fun onResume() {
        super.onResume()
        if (::map.isInitialized) map.onResume()
    }

    override fun onPause() {
        if (::map.isInitialized) map.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        if (::map.isInitialized) map.onDetach()
        super.onDestroy()
    }

    private fun loadStations() {
        findViewById<View>(R.id.mapProgress).visibility = View.VISIBLE
        lifecycleScope.launch {
            repository.dashboard()
                .onSuccess { dashboard ->
                    renderStations(dashboard.stations)
                    findViewById<TextView>(R.id.tvMapStatus).text =
                        getString(R.string.operator_map_station_count, dashboard.stations.size)
                }
                .onFailure { error ->
                    findViewById<TextView>(R.id.tvMapStatus).text = error.message
                        ?: getString(R.string.operator_map_load_failed)
                    showFailure(error)
                }
            findViewById<View>(R.id.mapProgress).visibility = View.GONE
        }
    }

    private fun renderStations(stations: List<com.smartsolar.microgrid.model.OperatorStation>) {
        map.overlays.removeAll { it is Marker }
        val validStations = stations.filter { it.latitude != 0.0 || it.longitude != 0.0 }
        validStations.forEach { station ->
            map.overlays.add(Marker(map).apply {
                position = GeoPoint(station.latitude, station.longitude)
                title = station.name
                snippet = "${station.stationId} · ${station.status}"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })
        }
        if (validStations.size == 1) {
            val station = validStations.first()
            map.controller.setCenter(GeoPoint(station.latitude, station.longitude))
            map.controller.setZoom(12.0)
        }
        map.invalidate()
    }
}