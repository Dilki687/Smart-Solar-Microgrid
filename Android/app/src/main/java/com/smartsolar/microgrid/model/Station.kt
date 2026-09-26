package com.smartsolar.microgrid.model

/**
 * Represents a solar microgrid station returned by the C# API.
 */
data class Station(
    val stationId: String,
    val name: String,
    val address: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val capacityKw: Double = 0.0,
    val status: String? = null,
    val operatorUserId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/**
 * Wrapper for GET /api/stations — the backend returns
 * `{ "stations": [ ... ] }`.
 */
data class StationsResponse(
    val stations: List<Station> = emptyList(),
)
