package com.smartsolar.microgrid.data.repository

import com.smartsolar.microgrid.data.remote.ApiClient
import com.smartsolar.microgrid.model.BookingSlot
import com.smartsolar.microgrid.model.CreateBookingSlotRequest
import com.smartsolar.microgrid.model.UpdateBookingSlotRequest
import org.json.JSONObject
import retrofit2.Response

/**
 * Booking-slot repository -- read for prosumers, and full CRUD
 * for backoffice/grid-operator officers.
 */
class BookingSlotRepository {

    suspend fun getSlots(
        stationId: String? = null,
    ): Result<List<BookingSlot>> = safe {
        ApiClient.bookingSlotApi.getSlots(stationId)
    }

    suspend fun create(
        request: CreateBookingSlotRequest,
    ): Result<Unit> = simple {
        ApiClient.bookingSlotApi.createSlot(request)
    }

    suspend fun update(
        slotId: String,
        request: UpdateBookingSlotRequest,
    ): Result<Unit> = simple {
        ApiClient.bookingSlotApi.updateSlot(slotId, request)
    }

    suspend fun deactivate(slotId: String): Result<Unit> =
        simple { ApiClient.bookingSlotApi.deactivateSlot(slotId) }

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
