package com.smartsolar.microgrid.ui.backoffice

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.ProsumerRepository
import com.smartsolar.microgrid.model.DeactivationRequest
import com.smartsolar.microgrid.utils.ReservationFormat
import kotlinx.coroutines.launch

/**
 * List of prosumer accounts awaiting Backoffice approval for
 * deactivation. Tapping Approve finalises the deactivation.
 */
class DeactivationRequestsActivity : BackofficeShellActivity() {

    override val contentLayoutRes = R.layout.activity_deactivation_requests
    override val currentSection = NavSection.DEACTIVATION

    private val repo = ProsumerRepository()

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: RequestsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        swipe = findViewById(R.id.swipeRefresh)
        rv = findViewById(R.id.rvList)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = RequestsAdapter { req -> confirmApprove(req) }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        swipe.setOnRefreshListener { load() }
        load()
    }

    private fun load() {
        swipe.isRefreshing = true
        lifecycleScope.launch {
            val result = repo.deactivationRequests()
            swipe.isRefreshing = false
            result.onSuccess { list ->
                adapter.submit(list)
                tvEmpty.visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure {
                adapter.submit(emptyList())
                tvEmpty.text = it.message
                    ?: getString(R.string.empty_deactivation)
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun confirmApprove(req: DeactivationRequest) {
        AlertDialog.Builder(this)
            .setMessage("Approve deactivation for ${req.name ?: req.nic}?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    repo.deactivate(req.nic).fold(
                        onSuccess = { load() },
                        onFailure = { showErr(it.message) },
                    )
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showErr(msg: String?) {
        AlertDialog.Builder(this)
            .setMessage(msg ?: "Something went wrong.")
            .setPositiveButton(R.string.action_close, null)
            .show()
    }

    private class RequestsAdapter(
        val onApprove: (DeactivationRequest) -> Unit,
    ) : RecyclerView.Adapter<RequestsAdapter.VH>() {

        private val items = mutableListOf<DeactivationRequest>()

        fun submit(list: List<DeactivationRequest>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_deactivation, parent, false),
            )

        override fun onBindViewHolder(holder: VH, position: Int) =
            holder.bind(items[position])

        override fun getItemCount() = items.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvName = v.findViewById<TextView>(R.id.tvName)
            private val tvMeta = v.findViewById<TextView>(R.id.tvMeta)
            private val btn = v.findViewById<MaterialButton>(R.id.btnApprove)

            fun bind(r: DeactivationRequest) {
                tvName.text = r.name ?: r.nic
                val when1 = r.requestedAt?.let {
                    "Requested: ${ReservationFormat.format(it)}"
                } ?: ""
                tvMeta.text = listOfNotNull(r.nic, r.email, when1)
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                btn.setOnClickListener { onApprove(r) }
            }
        }
    }
}
