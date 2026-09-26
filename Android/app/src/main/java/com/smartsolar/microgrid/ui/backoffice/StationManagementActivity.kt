package com.smartsolar.microgrid.ui.backoffice

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.StationRepository
import com.smartsolar.microgrid.model.CreateStationRequest
import com.smartsolar.microgrid.model.Station
import com.smartsolar.microgrid.model.UpdateStationRequest
import kotlinx.coroutines.launch

/**
 * List every station. FAB opens the create dialog; tap a row to
 * edit or deactivate.
 */
class StationManagementActivity : BackofficeShellActivity() {

    override val contentLayoutRes = R.layout.activity_station_management
    override val currentSection = NavSection.STATIONS

    private val repo = StationRepository()

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: StationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        swipe = findViewById(R.id.swipeRefresh)
        rv = findViewById(R.id.rvList)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = StationAdapter { s -> showActions(s) }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        swipe.setOnRefreshListener { load() }
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            showCreate()
        }
        load()
    }

    private fun load() {
        swipe.isRefreshing = true
        lifecycleScope.launch {
            val result = repo.getStations()
            swipe.isRefreshing = false
            result.onSuccess { list ->
                adapter.submit(list)
                tvEmpty.visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure {
                adapter.submit(emptyList())
                tvEmpty.text = it.message ?: getString(R.string.empty_stations)
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun showActions(station: Station) {
        val items = if (station.status == "ACTIVE")
            arrayOf("Edit", "Deactivate", "Close")
        else arrayOf("Edit", "Close")

        AlertDialog.Builder(this)
            .setTitle(station.name)
            .setItems(items) { _, which ->
                when {
                    which == 0 -> showEdit(station)
                    which == 1 && station.status == "ACTIVE" ->
                        confirmDeactivate(station)
                }
            }
            .show()
    }

    private fun confirmDeactivate(station: Station) {
        AlertDialog.Builder(this)
            .setMessage("Deactivate \"${station.name}\"?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    repo.deactivate(station.stationId).fold(
                        onSuccess = { load() },
                        onFailure = { err(it.message) },
                    )
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showCreate() {
        val form = StationForm(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.qa_stations)
            .setView(form.root)
            .setPositiveButton(R.string.action_create, null)
            .setNegativeButton("Cancel", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val v = form.read() ?: return@setOnClickListener
                        lifecycleScope.launch {
                            repo.create(
                                CreateStationRequest(
                                    name = v.name, address = v.address,
                                    latitude = v.lat, longitude = v.lng,
                                    capacityKw = v.capacityKw,
                                    operatorUserId = v.operator,
                                ),
                            ).fold(
                                onSuccess = { dismiss(); load() },
                                onFailure = { err(it.message) },
                            )
                        }
                    }
                }
            }.show()
    }

    private fun showEdit(station: Station) {
        val form = StationForm(this).apply { fill(station) }
        AlertDialog.Builder(this)
            .setTitle("Edit ${station.name}")
            .setView(form.root)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton("Cancel", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val v = form.read() ?: return@setOnClickListener
                        lifecycleScope.launch {
                            repo.update(
                                station.stationId,
                                UpdateStationRequest(
                                    name = v.name, address = v.address,
                                    latitude = v.lat, longitude = v.lng,
                                    capacityKw = v.capacityKw,
                                    operatorUserId = v.operator,
                                ),
                            ).fold(
                                onSuccess = { dismiss(); load() },
                                onFailure = { err(it.message) },
                            )
                        }
                    }
                }
            }.show()
    }

    private fun err(msg: String?) {
        AlertDialog.Builder(this)
            .setMessage(msg ?: "Something went wrong.")
            .setPositiveButton(R.string.action_close, null)
            .show()
    }

    private class StationAdapter(
        val onClick: (Station) -> Unit,
    ) : RecyclerView.Adapter<StationAdapter.VH>() {

        private val items = mutableListOf<Station>()

        fun submit(list: List<Station>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_station, parent, false),
            )

        override fun onBindViewHolder(holder: VH, position: Int) =
            holder.bind(items[position])

        override fun getItemCount() = items.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvName = v.findViewById<TextView>(R.id.tvName)
            private val tvMeta = v.findViewById<TextView>(R.id.tvMeta)
            private val tvStatus = v.findViewById<TextView>(R.id.tvStatus)

            fun bind(s: Station) {
                tvName.text = s.name
                tvMeta.text = buildString {
                    append(s.stationId)
                    append(" · %.1f kW".format(s.capacityKw))
                    s.address?.takeIf { it.isNotBlank() }?.let {
                        append("\n"); append(it)
                    }
                }
                tvStatus.text = s.status ?: "—"
                tvStatus.setBackgroundResource(
                    when (s.status) {
                        "ACTIVE" -> R.drawable.bg_badge_success
                        "INACTIVE" -> R.drawable.bg_badge_neutral
                        else -> R.drawable.bg_badge_neutral
                    },
                )
                itemView.setOnClickListener { onClick(s) }
            }
        }
    }
}

// ---------------------------------------------------------------
// Shared station form used for create + edit dialogs.
// ---------------------------------------------------------------

private class StationForm(ctx: Context) {

    val root: LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(ctx, 20), dp(ctx, 4), dp(ctx, 20), dp(ctx, 4))
    }

    private val name = field(ctx, "Name")
    private val address = field(ctx, "Address")
    private val lat = field(ctx, "Latitude", InputType.TYPE_CLASS_NUMBER or
            InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL)
    private val lng = field(ctx, "Longitude", InputType.TYPE_CLASS_NUMBER or
            InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL)
    private val cap = field(ctx, "Capacity (kW)", InputType.TYPE_CLASS_NUMBER or
            InputType.TYPE_NUMBER_FLAG_DECIMAL)
    private val op = field(ctx, "Operator User ID")

    init {
        listOf(name, address, lat, lng, cap, op).forEach { root.addView(it.til) }
    }

    fun fill(s: Station) {
        name.edit.setText(s.name)
        address.edit.setText(s.address.orEmpty())
        lat.edit.setText(s.latitude.toString())
        lng.edit.setText(s.longitude.toString())
        cap.edit.setText(s.capacityKw.toString())
        op.edit.setText(s.operatorUserId.orEmpty())
    }

    data class Values(
        val name: String, val address: String,
        val lat: Double, val lng: Double,
        val capacityKw: Double, val operator: String,
    )

    fun read(): Values? {
        val nameV = name.edit.text.toString().trim()
        val addressV = address.edit.text.toString().trim()
        val latV = lat.edit.text.toString().trim().toDoubleOrNull() ?: return null
        val lngV = lng.edit.text.toString().trim().toDoubleOrNull() ?: return null
        val capV = cap.edit.text.toString().trim().toDoubleOrNull() ?: return null
        val opV = op.edit.text.toString().trim()
        if (nameV.length < 2 || addressV.length < 5 || opV.isEmpty()) return null
        return Values(nameV, addressV, latV, lngV, capV, opV)
    }

    private data class F(val til: TextInputLayout, val edit: TextInputEditText)

    companion object {
        private fun dp(ctx: Context, v: Int): Int =
            (v * ctx.resources.displayMetrics.density).toInt()

        private fun field(ctx: Context, hint: String, inputType: Int = InputType.TYPE_CLASS_TEXT): F {
            val til = TextInputLayout(ctx).apply {
                this.hint = hint
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(ctx, 6) }
            }
            val et = TextInputEditText(til.context).apply { this.inputType = inputType }
            til.addView(et)
            return F(til, et)
        }
    }
}
