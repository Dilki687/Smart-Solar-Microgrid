package com.smartsolar.microgrid.model

/**
 * Represents a Prosumer account returned by the central API.
 */
data class Prosumer(

    // Unique user ID.
    val id: String,

    // Prosumer NIC.
    val nic: String,

    // Prosumer name.
    val name: String,

    // Email address.
    val email: String,

    // Phone number.
    val phone: String,

    // Residential/address information.
    val address: String,

    // Account status.
    val status: String,

    // Account creation date.
    val createdAt: String? = null
)