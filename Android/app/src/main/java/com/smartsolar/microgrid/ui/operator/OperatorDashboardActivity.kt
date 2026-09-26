package com.smartsolar.microgrid.ui.operator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.OperatorRepository
import com.smartsolar.microgrid.ui.operator.OperatorReservationsActivity
import kotlinx.coroutines.launch

class OperatorDashboardActivity : OperatorBaseActivity() {
    private val repo = OperatorRepository()
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessionValid()) return
        setContentView(R.layout.activity_operator_dashboard)
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
