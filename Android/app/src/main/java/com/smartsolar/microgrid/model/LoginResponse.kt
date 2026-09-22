package com.smartsolar.microgrid.model

/**
 * Represents the response returned by the login API.
 */
data class LoginResponse(

    // Message returned by the API.
    val message: String,

    // Authenticated user information.
    val user: User,

    // JWT access token.
    val token: String
)