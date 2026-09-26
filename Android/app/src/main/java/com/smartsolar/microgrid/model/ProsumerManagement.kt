package com.smartsolar.microgrid.model

/**
 * DTOs for backoffice-side Prosumer operations.
 */

data class RegisterProsumerRequest(
    val nic: String,
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val password: String,
)

/** A row in the "pending deactivation requests" list. */
data class DeactivationRequest(
    val nic: String,
    val name: String? = null,
    val email: String? = null,
    val requestedAt: String? = null,
    val status: String? = null,
)

data class DeactivationRequestsResponse(
    val requests: List<DeactivationRequest> = emptyList(),
)
