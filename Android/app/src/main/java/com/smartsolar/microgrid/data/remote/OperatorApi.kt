package com.smartsolar.microgrid.data.remote

import com.smartsolar.microgrid.model.*
import retrofit2.Response
import retrofit2.http.*

interface OperatorApi {
    @GET("api/operator/dashboard")
    suspend fun dashboard(): Response<OperatorDashboard>

    @POST("api/transactions/qr")
    suspend fun generateQr(@Body request: GenerateQrRequest): Response<ReservationQr>

    @POST("api/transactions/verify")
    suspend fun verify(@Body request: VerifyQrRequest): Response<TransferDetails>

    @POST("api/transactions/{id}/complete")
    suspend fun complete(@Path("id") id: String): Response<TransferDetails>

    @PATCH("api/stations/{id}/availability")
    suspend fun availability(@Path("id") id: String, @Body request: AvailabilityRequest): Response<AvailabilityResponse>
}
