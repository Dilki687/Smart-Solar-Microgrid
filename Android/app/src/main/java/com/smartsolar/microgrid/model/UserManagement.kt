package com.smartsolar.microgrid.model

/**
 * DTOs for the /api/users endpoints exposed to Backoffice staff.
 */

data class CreateUserRequest(
    val nic: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val address: String = "",
    val password: String,
    val role: String, // BACKOFFICE or GRID_OPERATOR
)

data class UpdateUserRequest(
    val name: String,
    val email: String,
    val phone: String = "",
    val address: String = "",
    val role: String,
)

data class UsersResponse(
    val users: List<User> = emptyList(),
)
