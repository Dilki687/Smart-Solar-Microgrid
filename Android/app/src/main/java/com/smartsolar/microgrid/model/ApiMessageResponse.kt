package com.smartsolar.microgrid.model

/**
 * Generic response returned by operations that mainly
 * return a success message.
 */
data class ApiMessageResponse(

    // Server response message.
    val message: String
)