package com.smartsolar.microgrid.model

/**
 * Represents an energy reservation returned by the C# Web API.
 *
 * The backend may serialise the Status field either as a string
 * ("PENDING") or as a numeric enum index (0..3), so both are
 * accepted at parse time and normalised through statusLabel().
 */
data class Reservation(

    val id: String? = null,

    val reservationId: String,

    val slotId: String,

    val stationId: String,

    val prosumerUserId: String,

    val scheduledStartTime: String,

    val scheduledEndTime: String,

    val energyAmountKwh: Double,

    // Deserialised as String or Number depending on backend config.
    val status: Any? = null,

    val hasPendingChange: Boolean = false,

    val changeRequestStatus: String? = null,

    val pendingSlotId: String? = null,

    val pendingStationId: String? = null,

    val pendingScheduledStartTime: String? = null,

    val pendingScheduledEndTime: String? = null,

    val pendingEnergyAmountKwh: Double? = null,

    val cancellationReason: String? = null,

    val createdAt: String? = null,

    val updatedAt: String? = null,
) {

    /**
     * Normalises the status field into one of PENDING,
     * CONFIRMED, COMPLETED, CANCELLED, or the raw text.
     */
    fun statusLabel(): String {
        return when (val raw = status) {
            is String -> raw.uppercase()
            is Number -> when (raw.toInt()) {
                0 -> "PENDING"
                1 -> "CONFIRMED"
                2 -> "COMPLETED"
                3 -> "CANCELLED"
                else -> raw.toString()
            }
            else -> raw?.toString().orEmpty()
        }
    }
}
