package com.smartsolar.microgrid.ui.operator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.ReservationRepository
import com.smartsolar.microgrid.model.Reservation
import com.smartsolar.microgrid.ui.adapter.ReservationsAdapter
import com.smartsolar.microgrid.ui.backoffice.BackofficeShellActivity
import com.smartsolar.microgrid.ui.reservation.ReservationDetailsActivity
import com.smartsolar.microgrid.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * Reservation management screen for BACKOFFICE and GRID_OPERATOR
 * users — status filter dropdown, list of every reservation,
 * tap opens the shared detail screen with officer-mode actions.
 *
 * Lives inside [BackofficeShellActivity] so the hamburger drawer
 * is available on this page too.
 */
class OperatorReservationsActivity : BackofficeShellActivity() {

    override val contentLayoutRes = R.layout.activity_operator_reservations
    override val currentSection = NavSection.RESERVATIONS

    private val repo = ReservationRepository()

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ReservationsAdapter
    private lateinit var spinner: Spinner

    private val statusOptions = listOf(
        "" to "All Statuses",
        "PENDING" to "Pending",
        "CONFIRMED" to "Confirmed",
        "COMPLETED" to "Completed",
        "CANCELLED" to "Cancelled",
    )

    private var currentStatus: String = ""
    private var viewerRole: String = "BACKOFFICE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewerRole =
            SessionManager(applicationContext).getUserRole()
                ?: "BACKOFFICE"

        swipe = findViewById(R.id.swipeRefresh)
        rv = findViewById(R.id.rvReservations)
        tvEmpty = findViewById(R.id.tvEmpty)
        spinner = findViewById(R.id.spinnerStatus)

        adapter = ReservationsAdapter { open(it) }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statusOptions.map { it.second },
        )
        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    currentStatus = statusOptions[position].first
                    load()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        swipe.setOnRefreshListener { load() }

        // Logout now lives in the drawer, so the in-page logout
        // button is hidden. The id is still in the layout so we
        // don't have to touch the XML in this refactor.
        findViewById<View>(R.id.btnLogout)?.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        swipe.isRefreshing = true

        lifecycleScope.launch {
            val result = repo.getAllReservations(
                status = currentStatus.ifBlank { null },
            )
            swipe.isRefreshing = false

            result.onSuccess { list ->
                adapter.submit(list)
                tvEmpty.visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
                if (list.isEmpty()) {
                    tvEmpty.text = getString(R.string.my_reservations_empty)
                }
            }.onFailure { throwable ->
                adapter.submit(emptyList())
                tvEmpty.text = throwable.message
                    ?: getString(R.string.my_reservations_empty)
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun open(reservation: Reservation) {
        val intent = Intent(this, ReservationDetailsActivity::class.java)
        intent.putExtra(
            ReservationDetailsActivity.EXTRA_RESERVATION_JSON,
            com.google.gson.Gson().toJson(reservation),
        )
        intent.putExtra(
            ReservationDetailsActivity.EXTRA_VIEWER_ROLE, viewerRole,
        )
        startActivity(intent)
    }
}
