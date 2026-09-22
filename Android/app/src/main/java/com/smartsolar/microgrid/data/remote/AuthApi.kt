package com.smartsolar.microgrid.data.remote

import com.smartsolar.microgrid.model.LoginRequest
import com.smartsolar.microgrid.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API definition for authentication-related operations.
 */
interface AuthApi {

    /**
     * Authenticates a user through the central C# Web API.
     *
     * Endpoint:
     * POST /api/auth/login
     */
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}