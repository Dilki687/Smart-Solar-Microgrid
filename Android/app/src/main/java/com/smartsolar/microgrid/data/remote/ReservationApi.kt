package com.smartsolar.microgrid.data.remote

import com.smartsolar.microgrid.model.ApiMessageResponse
import com.smartsolar.microgrid.model.CreateReservationRequest
import com.smartsolar.microgrid.model.ReasonRequest
import com.smartsolar.microgrid.model.ReservationsResponse
import com.smartsolar.microgrid.model.UpdateReservationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API definition for reservation operations.
 * Wraps every endpoint from BookingsController.cs that
 * concerns reservations.
 */
interface ReservationApi {

    /** GET /api/bookings/reservations?status=... — officer view. */
    @GET("api/bookings/reservations")
    suspend fun getAllReservations(
        @Query("status") status: String? = null,
    ): Response<ReservationsResponse>

    /** GET /api/bookings/reservations/my — prosumer view. */
    @GET("api/bookings/reservations/my")
    suspend fun getMyReservations(): Response<ReservationsResponse>

    /** POST /api/bookings/reservations — create a reservation. */
    @POST("api/bookings/reservations")
    suspend fun createReservation(
        @Body request: CreateReservationRequest,
    ): Response<ApiMessageResponse>

    /** POST /api/bookings/reservations/{id}/approve — officer only. */
    @POST("api/bookings/reservations/{reservationId}/approve")
    suspend fun approve(
        @Path("reservationId") reservationId: String,
    ): Response<ApiMessageResponse>

    /** POST /api/bookings/reservations/{id}/reject — officer only. */
    @POST("api/bookings/reservations/{reservationId}/reject")
    suspend fun reject(
        @Path("reservationId") reservationId: String,
        @Body request: ReasonRequest,
    ): Response<ApiMessageResponse>

    /** POST /api/bookings/reservations/{id}/cancel — prosumer only. */
    @POST("api/bookings/reservations/{reservationId}/cancel")
    suspend fun cancel(
        @Path("reservationId") reservationId: String,
        @Body request: ReasonRequest,
    ): Response<ApiMessageResponse>

    /** POST /api/bookings/reservations/{id}/request-update — prosumer. */
    @POST("api/bookings/reservations/{reservationId}/request-update")
    suspend fun requestUpdate(
        @Path("reservationId") reservationId: String,
        @Body request: UpdateReservationRequest,
    ): Response<ApiMessageResponse>

    /** POST /api/bookings/reservations/{id}/approve-change — officer. */
    @POST("api/bookings/reservations/{reservationId}/approve-change")
    suspend fun approveChange(
        @Path("reservationId") reservationId: String,
    ): Response<ApiMessageResponse>

    /** POST /api/bookings/reservations/{id}/reject-change — officer. */
    @POST("api/bookings/reservations/{reservationId}/reject-change")
    suspend fun rejectChange(
        @Path("reservationId") reservationId: String,
        @Body request: ReasonRequest,
    ): Response<ApiMessageResponse>
}
