package com.smartsolar.microgrid.model

data class OperatorStation(val stationId: String, val name: String, val status: String)
data class OperatorSlot(
    val slotId: String, val stationId: String, val startTime: String, val endTime: String,
    val totalCapacity: Int, val availableCapacity: Int,
)
data class OperatorDashboard(
    val operatorName: String, val timeZone: String,
    val todayBookings: Int, val upcomingBookings: Int, val pendingBookings: Int, val completedBookings: Int,
    val today: List<Reservation>, val upcoming: List<Reservation>,
    val stations: List<OperatorStation>, val slots: List<OperatorSlot>,
)
data class GenerateQrRequest(val reservationId: String)
data class VerifyQrRequest(val qrToken: String)
data class AvailabilityRequest(val slotId: String, val totalCapacity: Int)
data class AvailabilityResponse(val message: String, val slot: OperatorSlot)
data class ReservationQr(
    val transactionId: String, val reservationId: String, val qrToken: String,
    val qrStatus: String, val transactionStatus: String, val expiresAt: String,
)
data class TransferDetails(
    val transactionId: String, val reservationId: String,
    val prosumerNIC: String, val prosumerName: String?, val stationId: String, val stationName: String?,
    val slotId: String, val scheduledStartTime: String, val scheduledEndTime: String,
    val energyAmountKwh: Double, val reservationStatus: String, val transactionStatus: String,
    val qrStatus: String, val verifiedAt: String?, val completedAt: String?,
)
