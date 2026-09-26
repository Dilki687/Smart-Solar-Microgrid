package com.smartsolar.microgrid.model

/** DTOs for backoffice-side Station write operations. */

data class CreateStationRequest(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val capacityKw: Double,
    val operatorUserId: String,
)

data class UpdateStationRequest(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val capacityKw: Double,
    val operatorUserId: String,
)
