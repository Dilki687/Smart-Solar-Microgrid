package com.smartsolar.microgrid.data.repository

import com.smartsolar.microgrid.data.remote.ApiClient
import com.smartsolar.microgrid.model.DeactivationRequest
import com.smartsolar.microgrid.model.Prosumer
import com.smartsolar.microgrid.model.RegisterProsumerRequest
import org.json.JSONObject
import retrofit2.Response

/**
 * Backoffice-focused prosumer operations. The pre-existing
 * UserRepository stays for the prosumer's own self-service calls.
 */
class ProsumerRepository {

    suspend fun register(request: RegisterProsumerRequest): Result<Unit> =
        simple { ApiClient.prosumerApi.register(request) }

    suspend fun get(nic: String): Result<Prosumer> =
        safe { ApiClient.prosumerApi.getProsumer(nic) }

    suspend fun deactivationRequests(): Result<List<DeactivationRequest>> =
        safe { ApiClient.prosumerApi.getDeactivationRequests() }
            .map { it.requests }

    suspend fun deactivate(nic: String): Result<Unit> =
        simple { ApiClient.prosumerApi.deactivateProsumer(nic) }

    suspend fun reactivate(nic: String): Result<Unit> =
        simple { ApiClient.prosumerApi.reactivateProsumer(nic) }

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
