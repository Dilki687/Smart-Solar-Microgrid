package com.smartsolar.microgrid.data.repository

import com.smartsolar.microgrid.data.remote.ApiClient
import com.smartsolar.microgrid.model.ApiMessageResponse
import com.smartsolar.microgrid.model.Prosumer
import com.smartsolar.microgrid.model.UpdateProsumerRequest

/**
 * Repository responsible for Prosumer account operations.
 *
 * All server-side operations go through the central C# API.
 */
class UserRepository {

    /**
     * Retrieves a Prosumer profile using the NIC.
     */
    suspend fun getProsumer(
        nic: String
    ): Result<Prosumer> {

        return try {

            val response =
                ApiClient.prosumerApi.getProsumer(nic)

            if (response.isSuccessful &&
                response.body() != null
            ) {

                Result.success(response.body()!!)

            } else {

                Result.failure(
                    Exception(
                        getErrorMessage(response.code())
                    )
                )
            }

        } catch (exception: Exception) {

            Result.failure(
                Exception(
                    "Unable to connect to the server."
                )
            )
        }
    }

    /**
     * Updates a Prosumer's profile.
     */
    suspend fun updateProsumer(
        nic: String,
        request: UpdateProsumerRequest
    ): Result<ApiMessageResponse> {

        return try {

            val response =
                ApiClient.prosumerApi.updateProsumer(
                    nic,
                    request
                )

            if (response.isSuccessful) {

                Result.success(
                    response.body()
                        ?: ApiMessageResponse(
                            "Profile updated successfully."
                        )
                )

            } else {

                Result.failure(
                    Exception(
                        getErrorMessage(response.code())
                    )
                )
            }

        } catch (exception: Exception) {

            Result.failure(
                Exception(
                    "Unable to connect to the server."
                )
            )
        }
    }

    /**
     * Sends a Prosumer account deactivation request.
     */
    suspend fun requestDeactivation(
        nic: String
    ): Result<ApiMessageResponse> {

        return try {

            val response =
                ApiClient.prosumerApi.requestDeactivation(nic)

            if (response.isSuccessful) {

                Result.success(
                    response.body()
                        ?: ApiMessageResponse(
                            "Deactivation request submitted."
                        )
                )

            } else {

                Result.failure(
                    Exception(
                        getErrorMessage(response.code())
                    )
                )
            }

        } catch (exception: Exception) {

            Result.failure(
                Exception(
                    "Unable to connect to the server."
                )
            )
        }
    }

    /**
     * Converts HTTP error codes into readable messages.
     */
    private fun getErrorMessage(
        statusCode: Int
    ): String {

        return when (statusCode) {

            400 ->
                "Invalid request."

            401 ->
                "Your session has expired. Please log in again."

            403 ->
                "You are not authorized to perform this action."

            404 ->
                "Prosumer account was not found."

            409 ->
                "This operation cannot be completed because of the current account status."

            else ->
                "Server error: $statusCode"
        }
    }
}