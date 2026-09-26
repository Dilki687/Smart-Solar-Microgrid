package com.smartsolar.microgrid.model

/**
 * Request payload for POST /api/bookings/reservations.
 */
data class CreateReservationRequest(
    val slotId: String,
    val stationId: String,
    val scheduledStartTime: String,
    val scheduledEndTime: String,
    val energyAmountKwh: Double,
)

/**
 * Request payload for POST /api/bookings/reservations/{id}/request-update.
 */
data class UpdateReservationRequest(
    val slotId: String,
    val scheduledStartTime: String,
    val scheduledEndTime: String,
    val energyAmountKwh: Double,
)

/**
 * Request payload for reject / cancel operations that carry a reason.
 */
data class ReasonRequest(
    val reason: String,
)

/**
 * Wrapper the backend returns for reservation list endpoints:
 * `{ "reservations": [...] }`.
 */
data class ReservationsResponse(
    val reservations: List<Reservation> = emptyList(),
)
