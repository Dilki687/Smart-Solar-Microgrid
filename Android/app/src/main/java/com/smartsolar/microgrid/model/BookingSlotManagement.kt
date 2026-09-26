package com.smartsolar.microgrid.model

/** DTOs for backoffice-side BookingSlot write operations. */

data class CreateBookingSlotRequest(
    val stationId: String,
    val startTime: String,
    val endTime: String,
    val totalCapacity: Int,
)

data class UpdateBookingSlotRequest(
    val startTime: String,
    val endTime: String,
    val totalCapacity: Int,
)
