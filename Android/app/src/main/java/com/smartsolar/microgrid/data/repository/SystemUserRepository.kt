package com.smartsolar.microgrid.data.repository

import com.smartsolar.microgrid.data.remote.ApiClient
import com.smartsolar.microgrid.model.CreateUserRequest
import com.smartsolar.microgrid.model.UpdateUserRequest
import com.smartsolar.microgrid.model.User
import org.json.JSONObject
import retrofit2.Response

/**
 * Repository for backoffice management of BACKOFFICE and
 * GRID_OPERATOR user accounts. (Named "SystemUser" to distinguish
 * from the existing prosumer-focused UserRepository.)
 */
class SystemUserRepository {

    suspend fun list(
        role: String? = null,
        status: String? = null,
    ): Result<List<User>> = safe {
        ApiClient.userApi.getUsers(role, status)
    }.map { it.users }

    suspend fun create(request: CreateUserRequest): Result<Unit> =
        simple { ApiClient.userApi.createUser(request) }

    suspend fun update(
        userId: String,
        request: UpdateUserRequest,
    ): Result<Unit> =
        simple { ApiClient.userApi.updateUser(userId, request) }

    suspend fun deactivate(userId: String): Result<Unit> =
        simple { ApiClient.userApi.deactivateUser(userId) }

    // ---------- helpers ----------

    private suspend fun <T> safe(
        call: suspend () -> Response<T>,
    ): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(errorMessage(response)))
            }
        } catch (_: Throwable) {
            Result.failure(Exception("Unable to reach the server."))
        }
    }

    private suspend fun <T> simple(
        call: suspend () -> Response<T>,
    ): Result<Unit> = safe(call).map { }

    private fun errorMessage(response: Response<*>): String {
        val raw = response.errorBody()?.string()
        if (!raw.isNullOrBlank()) {
            try {
                val json = JSONObject(raw)
                if (json.has("message")) return json.getString("message")
                if (json.has("title")) return json.getString("title")
            } catch (_: Exception) {
            }
        }
        return "Request failed (HTTP ${response.code()})."
    }
}
