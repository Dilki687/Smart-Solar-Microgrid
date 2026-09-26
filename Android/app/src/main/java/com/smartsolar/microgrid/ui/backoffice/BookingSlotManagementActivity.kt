package com.smartsolar.microgrid.ui.backoffice

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.BookingSlotRepository
import com.smartsolar.microgrid.data.repository.StationRepository
import com.smartsolar.microgrid.model.BookingSlot
import com.smartsolar.microgrid.model.CreateBookingSlotRequest
import com.smartsolar.microgrid.model.Station
import com.smartsolar.microgrid.model.UpdateBookingSlotRequest
import com.smartsolar.microgrid.utils.ReservationFormat
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Backoffice / Grid-operator screen for CRUD-ing booking slots.
 * FAB creates a slot; tap a row for edit / deactivate.
 */
class BookingSlotManagementActivity : BackofficeShellActivity() {

    override val contentLayoutRes = R.layout.activity_booking_slot_management
    override val currentSection = NavSection.SLOTS

    private val slotRepo = BookingSlotRepository()
    private val stationRepo = StationRepository()

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: SlotAdapter

    private var stations: List<Station> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        swipe = findViewById(R.id.swipeRefresh)
        rv = findViewById(R.id.rvList)
        tvEmpty = findViewById(R.id.tvEmpty)

        adapter = SlotAdapter { slot -> showActions(slot) }
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
            val slotsD = async { slotRepo.getSlots() }
            val stationsD = async { stationRepo.getStations("ACTIVE") }

            stationsD.await().onSuccess { stations = it }
            val slotsResult = slotsD.await()
            swipe.isRefreshing = false

            slotsResult.onSuccess { list ->
                adapter.submit(list, stationLookup = stations.associateBy { it.stationId })
                tvEmpty.visibility =
                    if (list.isEmpty()) View.VISIBLE else View.GONE
            }.onFailure {
                adapter.submit(emptyList())
                tvEmpty.text = it.message ?: getString(R.string.empty_slots)
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun showActions(slot: BookingSlot) {
        val items = if (slot.isActive)
            arrayOf("Edit", "Deactivate", "Close")
        else arrayOf("Edit", "Close")

        AlertDialog.Builder(this)
            .setTitle("Slot ${slot.slotId.takeLast(6)}")
            .setItems(items) { _, which ->
                when {
                    which == 0 -> showEdit(slot)
                    which == 1 && slot.isActive -> confirmDeactivate(slot)
                }
            }
            .show()
    }

    private fun confirmDeactivate(slot: BookingSlot) {
        AlertDialog.Builder(this)
            .setMessage("Deactivate this slot?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    slotRepo.deactivate(slot.slotId).fold(
                        onSuccess = { load() },
                        onFailure = { err(it.message) },
                    )
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showCreate() {
        val form = SlotForm(this, stations, editing = null)
        AlertDialog.Builder(this)
            .setTitle(R.string.qa_slots)
            .setView(form.root)
            .setPositiveButton(R.string.action_create, null)
            .setNegativeButton("Cancel", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val v = form.read() ?: return@setOnClickListener
                        lifecycleScope.launch {
                            slotRepo.create(
                                CreateBookingSlotRequest(
                                    stationId = v.stationId,
                                    startTime = v.startIso,
                                    endTime = v.endIso,
                                    totalCapacity = v.capacity,
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

    private fun showEdit(slot: BookingSlot) {
        val form = SlotForm(this, stations, editing = slot)
        AlertDialog.Builder(this)
            .setTitle("Edit slot")
            .setView(form.root)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton("Cancel", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val v = form.read() ?: return@setOnClickListener
                        lifecycleScope.launch {
                            slotRepo.update(
                                slot.slotId,
                                UpdateBookingSlotRequest(
                                    startTime = v.startIso,
                                    endTime = v.endIso,
                                    totalCapacity = v.capacity,
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

    private class SlotAdapter(
        val onClick: (BookingSlot) -> Unit,
    ) : RecyclerView.Adapter<SlotAdapter.VH>() {

        private val items = mutableListOf<BookingSlot>()
        private var lookup: Map<String, Station> = emptyMap()

        fun submit(
            list: List<BookingSlot>,
            stationLookup: Map<String, Station> = lookup,
        ) {
            items.clear(); items.addAll(list)
            lookup = stationLookup
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_slot_manage, parent, false),
            )

        override fun onBindViewHolder(holder: VH, position: Int) =
            holder.bind(items[position])

        override fun getItemCount() = items.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            private val tvTitle = v.findViewById<TextView>(R.id.tvTitle)
            private val tvMeta = v.findViewById<TextView>(R.id.tvMeta)
            private val tvStatus = v.findViewById<TextView>(R.id.tvStatus)

            fun bind(s: BookingSlot) {
                val station = lookup[s.stationId]
                tvTitle.text = station?.name ?: s.stationId
                tvMeta.text = buildString {
                    append(ReservationFormat.format(s.startTime))
                    append(" -> ")
                    append(ReservationFormat.format(s.endTime))
                    append("\n${s.availableCapacity}/${s.totalCapacity} available")
                }
                tvStatus.text = if (s.isActive) "ACTIVE" else "INACTIVE"
                tvStatus.setBackgroundResource(
                    if (s.isActive) R.drawable.bg_badge_success
                    else R.drawable.bg_badge_neutral,
                )
                itemView.setOnClickListener { onClick(s) }
            }
        }
    }
}

// ---------------------------------------------------------------
// Slot create/edit form.
// ---------------------------------------------------------------

private class SlotForm(
    private val ctx: Context,
    stations: List<Station>,
    editing: BookingSlot?,
) {

    val root: LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(4), dp(20), dp(4))
    }

    private val stationChips = stations.filter { it.status == "ACTIVE" }
    private val stationBtn: MaterialButton
    private var chosenStationId: String? = editing?.stationId
        ?: stationChips.firstOrNull()?.stationId

    private val cap = field("Total capacity", InputType.TYPE_CLASS_NUMBER)

    private val startBtn = pickerBtn("Start")
    private val endBtn = pickerBtn("End")
    private var startCal: Calendar? = editing?.startTime?.let { calFromIso(it) }
    private var endCal: Calendar? = editing?.endTime?.let { calFromIso(it) }

    init {
        // Station picker (as a button that opens a chooser dialog)
        stationBtn = MaterialButton(ctx).apply {
            text = "Station"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(6) }
            isEnabled = editing == null // stationId is immutable on update
            setOnClickListener {
                if (stationChips.isEmpty()) return@setOnClickListener
                val labels = stationChips.map { "${it.name} (${it.stationId})" }
                    .toTypedArray()
                AlertDialog.Builder(ctx)
                    .setTitle("Choose station")
                    .setItems(labels) { _, i ->
                        chosenStationId = stationChips[i].stationId
                        text = "Station: ${stationChips[i].name}"
                    }.show()
            }
        }
        root.addView(stationBtn)

        // Times
        root.addView(startBtn)
        root.addView(endBtn)

        // Capacity
        root.addView(cap.til)

        // Initial state
        editing?.let {
            cap.edit.setText(it.totalCapacity.toString())
            startBtn.text = "Start: ${ReservationFormat.format(it.startTime)}"
            endBtn.text = "End: ${ReservationFormat.format(it.endTime)}"
            val stationName = stations.firstOrNull { s -> s.stationId == it.stationId }?.name
            stationBtn.text = "Station: ${stationName ?: it.stationId}"
        } ?: run {
            chosenStationId?.let { id ->
                val stationName = stations.firstOrNull { it.stationId == id }?.name
                stationBtn.text = "Station: ${stationName ?: id}"
            }
        }
    }

    data class Values(
        val stationId: String,
        val startIso: String,
        val endIso: String,
        val capacity: Int,
    )

    fun read(): Values? {
        val sid = chosenStationId ?: return null
        val start = startCal?.time ?: return null
        val end = endCal?.time ?: return null
        if (!end.after(start)) return null
        val capV = cap.edit.text.toString().trim().toIntOrNull() ?: return null
        if (capV <= 0) return null
        return Values(sid, toIso(start), toIso(end), capV)
    }

    private fun pickerBtn(label: String): MaterialButton =
        MaterialButton(ctx).apply {
            text = label
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(6) }
            setOnClickListener { pickDateTime(label) { cal ->
                if (label == "Start") startCal = cal else endCal = cal
                text = "$label: ${ReservationFormat.format(toIso(cal.time))}"
            } }
        }

    private fun pickDateTime(label: String, onPicked: (Calendar) -> Unit) {
        val now = Calendar.getInstance()
        DatePickerDialog(ctx, { _, y, m, d ->
            TimePickerDialog(ctx, { _, h, min ->
                val cal = Calendar.getInstance().apply {
                    set(y, m, d, h, min, 0); set(Calendar.MILLISECOND, 0)
                }
                onPicked(cal)
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH))
            .show()
    }

    private data class F(val til: TextInputLayout, val edit: TextInputEditText)

    private fun field(hint: String, inputType: Int = InputType.TYPE_CLASS_TEXT): F {
        val til = TextInputLayout(ctx).apply {
            this.hint = hint
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(6) }
        }
        val et = TextInputEditText(til.context).apply { this.inputType = inputType }
        til.addView(et)
        return F(til, et)
    }

    private fun dp(v: Int): Int =
        (v * ctx.resources.displayMetrics.density).toInt()

    companion object {
        private val outputIso = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US,
        ).apply { timeZone = TimeZone.getTimeZone("UTC") }

        fun toIso(date: Date): String = outputIso.format(date)

        fun calFromIso(iso: String): Calendar? {
            val d = ReservationFormat.parse(iso) ?: return null
            return Calendar.getInstance().apply { time = d }
        }
    }
}
