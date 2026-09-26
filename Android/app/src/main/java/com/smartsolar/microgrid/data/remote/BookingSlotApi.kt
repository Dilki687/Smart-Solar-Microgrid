package com.smartsolar.microgrid.data.remote

import com.smartsolar.microgrid.model.ApiMessageResponse
import com.smartsolar.microgrid.model.BookingSlot
import com.smartsolar.microgrid.model.CreateBookingSlotRequest
import com.smartsolar.microgrid.model.UpdateBookingSlotRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API for /api/bookings/slots -- prosumers read,
 * officers also create/update/deactivate.
 */
interface BookingSlotApi {

    /** GET /api/bookings/slots?stationId=... */
    @GET("api/bookings/slots")
    suspend fun getSlots(
        @Query("stationId") stationId: String? = null,
    ): Response<List<BookingSlot>>

    /** POST /api/bookings/slots */
    @POST("api/bookings/slots")
    suspend fun createSlot(
        @Body request: CreateBookingSlotRequest,
    ): Response<ApiMessageResponse>

    /** PUT /api/bookings/slots/{slotId} */
    @PUT("api/bookings/slots/{slotId}")
    suspend fun updateSlot(
        @Path("slotId") slotId: String,
        @Body request: UpdateBookingSlotRequest,
    ): Response<ApiMessageResponse>

    /** PATCH /api/bookings/slots/{slotId}/deactivate */
    @PATCH("api/bookings/slots/{slotId}/deactivate")
    suspend fun deactivateSlot(
        @Path("slotId") slotId: String,
    ): Response<ApiMessageResponse>
}
