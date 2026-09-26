package com.smartsolar.microgrid.data.remote

import com.smartsolar.microgrid.model.ApiMessageResponse
import com.smartsolar.microgrid.model.CreateStationRequest
import com.smartsolar.microgrid.model.Station
import com.smartsolar.microgrid.model.StationsResponse
import com.smartsolar.microgrid.model.UpdateStationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API for /api/stations.
 * Prosumers can read; officers can also create / update / deactivate.
 */
interface StationApi {

    @GET("api/nodes/nearby")
    suspend fun getNearbyStations(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Double,
    ): Response<com.smartsolar.microgrid.model.NearbyStationsResponse>

    /** GET /api/stations?status=ACTIVE */
    @GET("api/stations")
    suspend fun getStations(
        @Query("status") status: String? = null,
    ): Response<StationsResponse>

    /** GET /api/stations/{stationId} */
    @GET("api/stations/{stationId}")
    suspend fun getStation(
        @Path("stationId") stationId: String,
    ): Response<Station>

    /** POST /api/stations */
    @POST("api/stations")
    suspend fun createStation(
        @Body request: CreateStationRequest,
    ): Response<ApiMessageResponse>

    /** PUT /api/stations/{stationId} */
    @PUT("api/stations/{stationId}")
    suspend fun updateStation(
        @Path("stationId") stationId: String,
        @Body request: UpdateStationRequest,
    ): Response<ApiMessageResponse>

    /** PATCH /api/stations/{stationId}/deactivate */
    @PATCH("api/stations/{stationId}/deactivate")
    suspend fun deactivateStation(
        @Path("stationId") stationId: String,
    ): Response<ApiMessageResponse>
}
