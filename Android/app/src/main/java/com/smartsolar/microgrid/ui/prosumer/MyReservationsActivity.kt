package com.smartsolar.microgrid.ui.prosumer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.ReservationRepository
import com.smartsolar.microgrid.model.Reservation
import com.smartsolar.microgrid.ui.adapter.ReservationsAdapter
import com.smartsolar.microgrid.ui.reservation.ReservationDetailsActivity
import kotlinx.coroutines.launch

/**
 * Prosumer "My Reservations" list — hits /reservations/my and
 * opens the shared detail screen for a tap.
 */
class MyReservationsActivity : ProsumerShellActivity() {

    override val contentLayoutRes: Int = R.layout.activity_my_reservations
    override val currentSection: NavSection = NavSection.MY_RESERVATIONS

    private val repo = ReservationRepository()

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ReservationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        swipe = findViewById(R.id.swipeRefresh)
        rv = findViewById(R.id.rvReservations)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = ReservationsAdapter { open(it) }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        swipe.setOnRefreshListener { load() }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        swipe.isRefreshing = true

        lifecycleScope.launch {
            val result = repo.getMyReservations()
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
            ReservationDetailsActivity.EXTRA_VIEWER_ROLE, "PROSUMER",
        )
        startActivity(intent)
    }
}
