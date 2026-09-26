package com.smartsolar.microgrid.data.repository

import com.smartsolar.microgrid.data.remote.ApiClient
import com.smartsolar.microgrid.model.CreateStationRequest
import com.smartsolar.microgrid.model.Station
import com.smartsolar.microgrid.model.UpdateStationRequest
import org.json.JSONObject
import retrofit2.Response

/**
 * Repository for station read/write on the Android app.
 */
class StationRepository {

    suspend fun getStations(
        status: String? = null,
    ): Result<List<Station>> = safe {
        ApiClient.stationApi.getStations(status)
    }.map { it.stations }

    suspend fun get(stationId: String): Result<Station> =
        safe { ApiClient.stationApi.getStation(stationId) }

    suspend fun create(request: CreateStationRequest): Result<Unit> =
        simple { ApiClient.stationApi.createStation(request) }

    suspend fun update(
        stationId: String,
        request: UpdateStationRequest,
    ): Result<Unit> = simple {
        ApiClient.stationApi.updateStation(stationId, request)
    }

    suspend fun deactivate(stationId: String): Result<Unit> =
        simple { ApiClient.stationApi.deactivateStation(stationId) }

    // ---------- helpers ----------

    private suspend fun <T> safe(
        call: suspend () -> Response<T>,
    ): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(errorMessage(response)))
            }
        } catch (_: Throwable) {
            Result.failure(Exception("Unable to reach the server."))
        }
    }

    private suspend fun <T> simple(
        call: suspend () -> Response<T>,
    ): Result<Unit> = safe(call).map { }

    private fun errorMessage(response: Response<*>): String {
        val raw = response.errorBody()?.string()
        if (!raw.isNullOrBlank()) {
            try {
                val json = JSONObject(raw)
                if (json.has("message")) return json.getString("message")
            } catch (_: Exception) {
            }
        }
        return "Request failed (HTTP ${response.code()})."
    }
}
