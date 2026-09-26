package com.smartsolar.microgrid.data.repository

import com.smartsolar.microgrid.data.remote.ApiClient
import com.smartsolar.microgrid.model.*
import kotlinx.coroutines.CancellationException
import retrofit2.Response

class OperatorApiException(val status: Int, message: String) : Exception(message)

class OperatorRepository {
    suspend fun dashboard() = request { ApiClient.operatorApi.dashboard() }
    suspend fun generateQr(id: String) = request { ApiClient.operatorApi.generateQr(GenerateQrRequest(id)) }
    suspend fun verify(token: String) = request { ApiClient.operatorApi.verify(VerifyQrRequest(token)) }
    suspend fun complete(id: String) = request { ApiClient.operatorApi.complete(id) }
    suspend fun availability(stationId: String, slotId: String, total: Int) =
        request { ApiClient.operatorApi.availability(stationId, AvailabilityRequest(slotId, total)) }

    private suspend fun <T> request(block: suspend () -> Response<T>): Result<T> = try {
        val response = block()
        val body = response.body()
        if (response.isSuccessful && body != null) Result.success(body)
        else Result.failure(OperatorApiException(response.code(), messageFor(response.code())))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        Result.failure(OperatorApiException(0, "Unable to connect. Check your connection and try again."))
    }

    companion object {
        fun messageFor(status: Int): String = when (status) {
            400 -> "Invalid QR or input. Check the details and try again."
            401 -> "Your session has expired. Please log in again."
            403 -> "Your account or station assignment does not allow this action."
            404 -> "The reservation, transaction, or station slot was not found."
            409 -> "Already completed, expired, or conflicting reservation state. Refresh or scan again."
            else -> "The server could not complete this action. Please try again."
        }
    }
}
