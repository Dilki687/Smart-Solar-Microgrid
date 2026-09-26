package com.smartsolar.microgrid.model

/**
 * Represents an energy booking slot published by a station.
 */
data class BookingSlot(

    val id: String? = null,

    val slotId: String,

    val stationId: String,

    val startTime: String,

    val endTime: String,

    val totalCapacity: Int,

    val availableCapacity: Int,

    val isActive: Boolean,

    val createdAt: String? = null,

    val updatedAt: String? = null,
)
