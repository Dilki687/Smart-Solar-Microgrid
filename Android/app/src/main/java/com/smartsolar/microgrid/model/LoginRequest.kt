package com.smartsolar.microgrid.model

/**
 * Request model used when a user logs into the system.
 *
 * The identifier can be a NIC or email address.
 */
data class LoginRequest(

    // NIC or email entered by the user.
    val identifier: String,

    // Password entered by the user.
    val password: String
)