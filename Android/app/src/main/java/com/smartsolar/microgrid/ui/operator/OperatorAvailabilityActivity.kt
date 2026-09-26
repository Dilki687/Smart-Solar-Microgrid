package com.smartsolar.microgrid.ui.operator

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.smartsolar.microgrid.R
import com.smartsolar.microgrid.data.repository.OperatorRepository
import com.smartsolar.microgrid.model.OperatorSlot
import com.smartsolar.microgrid.utils.ReservationFormat
import kotlinx.coroutines.launch

class OperatorAvailabilityActivity : OperatorBaseActivity() {
    private val repo = OperatorRepository()
    private var slots = emptyList<OperatorSlot>()
    private lateinit var spinner: Spinner
    private lateinit var capacity: EditText
    private lateinit var save: Button
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sessionValid()) return
        setContentView(R.layout.activity_operator_availability)
        spinner = findViewById(R.id.spinnerOperatorSlot)
        capacity = findViewById(R.id.etOperatorCapacity)
        save = findViewById(R.id.btnSaveAvailability)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val slot = slots.getOrNull(position) ?: return
                capacity.setText(slot.totalCapacity.toString())
                findViewById<TextView>(R.id.tvAvailabilityMessage).text = getString(
                    R.string.operator_capacity_detail, slot.availableCapacity, slot.totalCapacity - slot.availableCapacity,
                )
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        save.setOnClickListener { update() }
        findViewById<View>(R.id.btnReloadAvailability).setOnClickListener { load() }
        load()
    }

    private fun load() {
        if (busy) return
        setBusy(true)
        lifecycleScope.launch {
            repo.dashboard().onSuccess { dashboard ->
                slots = dashboard.slots
                spinner.adapter = ArrayAdapter(this@OperatorAvailabilityActivity,
                    android.R.layout.simple_spinner_dropdown_item, slots.map { slot ->
                        val station = dashboard.stations.find { it.stationId == slot.stationId }
                        "${station?.name ?: slot.stationId} · ${ReservationFormat.format(slot.startTime)} · ${slot.slotId}"
                    })
                if (slots.isEmpty()) findViewById<TextView>(R.id.tvAvailabilityMessage).setText(R.string.operator_no_slots)
            }.onFailure { slots = emptyList(); showFailure(it) }
            setBusy(false)
        }
    }

    private fun update() {
        if (busy) return
        val slot = slots.getOrNull(spinner.selectedItemPosition) ?: return
        val total = capacity.text.toString().toIntOrNull()
        if (total == null || total <= 0 || total < slot.totalCapacity - slot.availableCapacity) {
            capacity.error = getString(R.string.operator_capacity_invalid); return
        }
        setBusy(true)
        lifecycleScope.launch {
            repo.availability(slot.stationId, slot.slotId, total).onSuccess {
                Toast.makeText(this@OperatorAvailabilityActivity, R.string.operator_availability_saved, Toast.LENGTH_SHORT).show()
            }.onFailure { showFailure(it) }
            setBusy(false)
            load()
        }
    }

    private fun setBusy(value: Boolean) {
        busy = value
        save.isEnabled = !value && slots.isNotEmpty()
        spinner.isEnabled = !value
        capacity.isEnabled = !value
        findViewById<View>(R.id.availabilityProgress).visibility = if (value) View.VISIBLE else View.GONE
    }
}
