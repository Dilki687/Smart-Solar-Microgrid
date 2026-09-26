package com.smartsolar.microgrid.ui.operator

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.OperatorRepository
import com.smartsolar.microgrid.data.repository.StationRepository
import com.smartsolar.microgrid.model.NearbyStation
import kotlinx.coroutines.launch

class OperatorDashboardActivity : OperatorBaseActivity() {
    private val repo = OperatorRepository()
    private val stationRepository = StationRepository()
    private var loading = false
    private var locationDialog: AlertDialog? = null
    private var latitudeInput: EditText? = null
    private var longitudeInput: EditText? = null
    private var locationListener: LocationListener? = null
    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) populateCurrentLocation()
        else Toast.makeText(this, "Location permission was not granted.", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessionValid()) return
        setContentView(R.layout.activity_operator_dashboard)
        val openScanner = View.OnClickListener { startActivity(Intent(this, QrTransferActivity::class.java)) }
        findViewById<View>(R.id.btnBottomScan).setOnClickListener(openScanner)
        findViewById<View>(R.id.btnBottomMap).setOnClickListener {
            startActivity(Intent(this, OperatorMapActivity::class.java))
        }
        findViewById<View>(R.id.btnNearbyStations).setOnClickListener { showNearbyStationsDialog() }
        findViewById<View>(R.id.btnRefresh).setOnClickListener { load() }
        findViewById<View>(R.id.btnOperatorLogout).setOnClickListener {
            startActivity(Intent(this, OperatorProfileActivity::class.java))
        }
    }

    private fun showNearbyStationsDialog() {
        val latitude = EditText(this).apply {
            hint = "Latitude (e.g. 6.9271)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        val longitude = EditText(this).apply {
            hint = "Longitude (e.g. 79.8612)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        val radius = EditText(this).apply {
            hint = "Radius in km (e.g. 10)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        latitudeInput = latitude
        longitudeInput = longitude
        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 0, 48, 0)
            addView(latitude)
            addView(longitude)
            addView(radius)
        }
        locationDialog = AlertDialog.Builder(this)
            .setTitle("Find nearby Stations")
            .setMessage("Enter your location and search radius.")
            .setView(fields)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Use my location") { _, _ -> requestCurrentLocation() }
            .setPositiveButton("Find Stations") { _, _ ->
                val lat = latitude.text.toString().toDoubleOrNull()
                val lon = longitude.text.toString().toDoubleOrNull()
                val range = radius.text.toString().toDoubleOrNull()
                if (lat == null || lon == null || range == null || lat !in -90.0..90.0 || lon !in -180.0..180.0 || range <= 0.0) {
                    Toast.makeText(this, "Enter valid latitude, longitude, and radius.", Toast.LENGTH_LONG).show()
                } else {
                    findNearbyStations(lat, lon, range)
                }
            }
            .show()
        locationDialog?.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
            requestCurrentLocation()
        }
    }

    private fun requestCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) populateCurrentLocation()
        else locationPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    private fun populateCurrentLocation() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val location = providers.asSequence()
            .filter { provider -> locationManager.isProviderEnabled(provider) }
            .mapNotNull { provider ->
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) null else locationManager.getLastKnownLocation(provider)
            }
            .maxByOrNull { it.time }
        if (location == null) {
            requestFreshLocation(locationManager, providers)
            return
        }
        fillLocation(location)
    }

    private fun requestFreshLocation(locationManager: LocationManager, providers: List<String>) {
        val availableProviders = providers.filter { provider ->
            locationManager.isProviderEnabled(provider)
        }
        if (availableProviders.isEmpty()) {
            Toast.makeText(this, "Enable phone location and try again.", Toast.LENGTH_LONG).show()
            return
        }
        locationListener?.let { listener ->
            availableProviders.forEach { provider -> locationManager.removeUpdates(listener) }
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                locationListener = null
                fillLocation(location)
            }
        }
        locationListener = listener
        try {
            availableProviders.forEach { provider ->
                locationManager.requestLocationUpdates(provider, 0L, 0f, listener, mainLooper)
            }
            Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "Location permission is required.", Toast.LENGTH_LONG).show()
        }
    }

    private fun fillLocation(location: Location) {
        latitudeInput?.setText("%.6f".format(java.util.Locale.US, location.latitude))
        longitudeInput?.setText("%.6f".format(java.util.Locale.US, location.longitude))
        Toast.makeText(this, "Location filled automatically.", Toast.LENGTH_SHORT).show()
    }

    private fun findNearbyStations(latitude: Double, longitude: Double, radiusKm: Double) {
        lifecycleScope.launch {
            stationRepository.getNearbyStations(latitude, longitude, radiusKm)
                .onSuccess { stations -> showNearbyResults(stations, radiusKm) }
                .onFailure { Toast.makeText(this@OperatorDashboardActivity, it.message ?: "Unable to find stations.", Toast.LENGTH_LONG).show() }
        }
    }

    private fun showNearbyResults(stations: List<NearbyStation>, radiusKm: Double) {
        val message = if (stations.isEmpty()) {
            "No active microgrid stations were found within ${radiusKm.formatOneDecimal()} km."
        } else {
            stations.joinToString("\n\n") {
                "${it.name}\n${it.distanceKm.formatOneDecimal()} km away · ${it.availableSlots} available slots\n${it.status}"
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Nearby Stations (${stations.size})")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun Double.formatOneDecimal(): String = String.format(java.util.Locale.US, "%.1f", this)

    override fun onResume() {
        super.onResume()
        if (!isFinishing) load()
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
                findViewById<TextView>(R.id.tvTotalStations).text = dashboard.totalStations.toString()
                findViewById<TextView>(R.id.tvActiveStations).text = dashboard.activeStations.toString()
                findViewById<TextView>(R.id.tvInactiveStations).text = dashboard.inactiveStations.toString()
                findViewById<TextView>(R.id.tvAvailableCapacity).text = dashboard.availableSlotCapacity.toString()
            }.onFailure {
                showFailure(it)
            }
        }
    }
}
