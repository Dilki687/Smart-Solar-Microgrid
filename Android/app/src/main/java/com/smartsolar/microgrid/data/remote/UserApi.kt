package com.smartsolar.microgrid.data.remote

import com.smartsolar.microgrid.model.ApiMessageResponse
import com.smartsolar.microgrid.model.CreateUserRequest
import com.smartsolar.microgrid.model.UpdateUserRequest
import com.smartsolar.microgrid.model.UsersResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API for /api/users -- Backoffice-only management of
 * BACKOFFICE and GRID_OPERATOR accounts.
 */
interface UserApi {

    @GET("api/users")
    suspend fun getUsers(
        @Query("role") role: String? = null,
        @Query("status") status: String? = null,
    ): Response<UsersResponse>

    @POST("api/users")
    suspend fun createUser(
        @Body request: CreateUserRequest,
    ): Response<ApiMessageResponse>

    @PUT("api/users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: String,
        @Body request: UpdateUserRequest,
    ): Response<ApiMessageResponse>

    @PATCH("api/users/{userId}/deactivate")
    suspend fun deactivateUser(
        @Path("userId") userId: String,
    ): Response<ApiMessageResponse>
}
