package com.smartsolar.microgrid.data.repository

import com.smartsolar.microgrid.data.remote.ApiClient
import com.smartsolar.microgrid.model.CreateReservationRequest
import com.smartsolar.microgrid.model.ReasonRequest
import com.smartsolar.microgrid.model.Reservation
import com.smartsolar.microgrid.model.UpdateReservationRequest
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.json.JSONObject
import retrofit2.Response

/**
 * Repository for reservation operations. Wraps the Retrofit
 * calls in the app's Result<T> convention and pulls a human
 * readable message out of the backend error body when it can.
 */
class ReservationRepository {

    /** Called by officers (Backoffice / Grid Operator). */
    suspend fun getAllReservations(
        status: String? = null,
    ): Result<List<Reservation>> = safeApiCall {
        ApiClient.reservationApi.getAllReservations(status)
    }.map { body -> body.reservations }

    /** Called by prosumers. */
    suspend fun getMyReservations(): Result<List<Reservation>> =
        safeApiCall { ApiClient.reservationApi.getMyReservations() }
            .map { body -> body.reservations }

    suspend fun createReservation(
        request: CreateReservationRequest,
    ): Result<Unit> = simpleCall {
        ApiClient.reservationApi.createReservation(request)
    }

    suspend fun approve(reservationId: String): Result<Unit> =
        simpleCall { ApiClient.reservationApi.approve(reservationId) }

    suspend fun reject(
        reservationId: String,
        reason: String,
    ): Result<Unit> = simpleCall {
        ApiClient.reservationApi.reject(
            reservationId, ReasonRequest(reason),
        )
    }

    suspend fun cancel(
        reservationId: String,
        reason: String,
    ): Result<Unit> = simpleCall {
        ApiClient.reservationApi.cancel(
            reservationId, ReasonRequest(reason),
        )
    }

    suspend fun requestUpdate(
        reservationId: String,
        request: UpdateReservationRequest,
    ): Result<Unit> = simpleCall {
        ApiClient.reservationApi.requestUpdate(reservationId, request)
    }

    suspend fun approveChange(reservationId: String): Result<Unit> =
        simpleCall { ApiClient.reservationApi.approveChange(reservationId) }

    suspend fun rejectChange(
        reservationId: String,
        reason: String,
    ): Result<Unit> = simpleCall {
        ApiClient.reservationApi.rejectChange(
            reservationId, ReasonRequest(reason),
        )
    }

    // ---------- helpers ----------

    /**
     * Runs the API call, unwraps the body on 2xx, and turns non-2xx
     * responses into a Result.failure carrying the server's message
     * when one is provided as { "message": "..." }.
     */
    private suspend fun <T> safeApiCall(
        call: suspend () -> Response<T>,
    ): Result<T> {
        return try {
            val response = call()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(errorMessage(response)))
            }
        } catch (throwable: Throwable) {
            Result.failure(
                Exception("Unable to reach the server."),
            )
        }
    }

    /**
     * Variant that only cares whether the call succeeded (no useful
     * body to return — used for approve / reject / cancel calls).
     */
    private suspend fun <T> simpleCall(
        call: suspend () -> Response<T>,
    ): Result<Unit> = safeApiCall(call).map { }

    private fun errorMessage(response: Response<*>): String {
        val raw = response.errorBody()?.string()
        if (!raw.isNullOrBlank()) {
            try {
                val json = JSONObject(raw)
                if (json.has("message")) {
                    return json.getString("message")
                }
            } catch (_: JsonSyntaxException) {
            } catch (_: Exception) {
            }
        }
        return "Request failed (HTTP ${response.code()})."
    }

    // Kept so the Gson dependency isn't stripped by shrinkers if
    // we later switch to typed error DTOs.
    @Suppress("unused")
    private val gson = Gson()
}
