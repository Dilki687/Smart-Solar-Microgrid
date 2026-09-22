package com.smartsolar.microgrid.model

/**
 * Represents a user returned by the Smart Solar Microgrid API.
 */
data class User(

    // Unique system-generated user ID.
    val id: String,

    // User's NIC.
    val nic: String,

    // User's display name.
    val name: String,

    // User's email address.
    val email: String,

    // User role.
    // Possible values:
    // BACKOFFICE
    // GRID_OPERATOR
    // PROSUMER
    val role: String,

    // Account status.
    // Possible values:
    // ACTIVE
    // INACTIVE
    // PENDING_DEACTIVATION
    val status: String
)