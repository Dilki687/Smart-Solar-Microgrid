package com.smartsolar.microgrid.model

/**
 * Request body used to update a Prosumer's profile.
 */
data class UpdateProsumerRequest(

    // Updated name.
    val name: String,

    // Updated email.
    val email: String,

    // Updated phone number.
    val phone: String,

    // Updated address.
    val address: String
)