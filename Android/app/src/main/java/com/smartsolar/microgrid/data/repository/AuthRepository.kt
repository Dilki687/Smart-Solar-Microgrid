package com.smartsolar.microgrid.data.repository

import com.smartsolar.microgrid.data.local.UserDao
import com.smartsolar.microgrid.data.remote.ApiClient
import com.smartsolar.microgrid.model.LoginRequest
import com.smartsolar.microgrid.model.LoginResponse

/**
 * Repository responsible for authentication operations.
 *
 * The repository connects the UI with:
 *
 * 1. Central C# Web API
 * 2. Local SQLite database
 */
class AuthRepository(
    private val userDao: UserDao
) {

    /**
     * Sends login credentials to the central C# API.
     *
     * If authentication succeeds, the returned user and JWT
     * are stored locally in SQLite.
     */
    suspend fun login(
        identifier: String,
        password: String
    ): Result<LoginResponse> {

        return try {

            // Create login request.
            val request = LoginRequest(
                identifier = identifier,
                password = password
            )

            // Send request to the C# API.
            val response = ApiClient.authApi.login(request)

            if (response.isSuccessful && response.body() != null) {

                val loginResponse = response.body()!!

                /*
                 * Save authenticated user and JWT locally.
                 */
                userDao.saveUser(
                    user = loginResponse.user,
                    token = loginResponse.token
                )

                Result.success(loginResponse)

            } else {

                /*
                 * Return a readable error when the server rejects
                 * the credentials.
                 */
                val errorMessage = when (response.code()) {
                    400 -> "Invalid login details."
                    401 -> "Invalid username or password."
                    403 -> "Your account is not allowed to log in."
                    else -> "Login failed. Server error: ${response.code()}"
                }

                Result.failure(Exception(errorMessage))
            }

        } catch (exception: Exception) {

            /*
             * Handles network errors, connection failures,
             * JSON parsing errors, etc.
             */
            Result.failure(
                Exception(
                    "Unable to connect to the server. " +
                            "Please check the API connection."
                )
            )
        }
    }

    /**
     * Returns the locally stored user.
     */
    fun getCurrentUser() = userDao.getUser()

    /**
     * Returns the locally stored JWT token.
     */
    fun getToken() = userDao.getToken()

    /**
     * Clears the local authentication session.
     */
    fun logout() {
        userDao.clearUser()
    }
}