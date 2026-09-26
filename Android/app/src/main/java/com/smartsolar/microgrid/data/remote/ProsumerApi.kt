package com.smartsolar.microgrid.data.remote

import com.smartsolar.microgrid.model.ApiMessageResponse
import com.smartsolar.microgrid.model.DeactivationRequestsResponse
import com.smartsolar.microgrid.model.Prosumer
import com.smartsolar.microgrid.model.RegisterProsumerRequest
import com.smartsolar.microgrid.model.UpdateProsumerRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit API definition for Prosumer operations.
 */
interface ProsumerApi {

    /**
     * Gets a Prosumer by NIC.
     *
     * GET /api/prosumers/{nic}
     */
    @GET("api/prosumers/{nic}")
    suspend fun getProsumer(
        @Path("nic") nic: String
    ): Response<Prosumer>

    /**
     * Updates the authenticated Prosumer's profile.
     *
     * PUT /api/prosumers/{nic}
     */
    @PUT("api/prosumers/{nic}")
    suspend fun updateProsumer(
        @Path("nic") nic: String,
        @Body request: UpdateProsumerRequest
    ): Response<ApiMessageResponse>

    /**
     * Requests account deactivation.
     *
     * PATCH /api/prosumers/{nic}/deactivation-request
     */
    @PATCH("api/prosumers/{nic}/deactivation-request")
    suspend fun requestDeactivation(
        @Path("nic") nic: String
    ): Response<ApiMessageResponse>

    // ---- Backoffice-only endpoints ----

    /** POST /api/prosumers -- register a new prosumer. */
    @POST("api/prosumers")
    suspend fun register(
        @Body request: RegisterProsumerRequest,
    ): Response<ApiMessageResponse>

    /** GET /api/prosumers/deactivation-requests */
    @GET("api/prosumers/deactivation-requests")
    suspend fun getDeactivationRequests(): Response<DeactivationRequestsResponse>

    /** PATCH /api/prosumers/{nic}/deactivate */
    @PATCH("api/prosumers/{nic}/deactivate")
    suspend fun deactivateProsumer(
        @Path("nic") nic: String,
    ): Response<ApiMessageResponse>

    /** PATCH /api/prosumers/{nic}/reactivate */
    @PATCH("api/prosumers/{nic}/reactivate")
    suspend fun reactivateProsumer(
        @Path("nic") nic: String,
    ): Response<ApiMessageResponse>
}