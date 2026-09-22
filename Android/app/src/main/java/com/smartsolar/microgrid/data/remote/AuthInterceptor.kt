package com.smartsolar.microgrid.data.remote

import android.content.Context
import com.smartsolar.microgrid.data.local.UserDao
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the currently logged-in user's JWT token to
 * authenticated API requests.
 */
class AuthInterceptor(context: Context) : Interceptor {

    private val userDao = UserDao(context.applicationContext)

    override fun intercept(chain: Interceptor.Chain): Response {

        // Get the original request.
        val originalRequest = chain.request()

        // Retrieve the locally stored JWT.
        val token = userDao.getToken()

        /*
         * If there is no token, send the request normally.
         * This is useful for public endpoints such as login.
         */
        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        /*
         * Add the JWT Authorization header to the request.
         */
        val authenticatedRequest = originalRequest.newBuilder()
            .addHeader(
                "Authorization",
                "Bearer $token"
            )
            .build()

        // Continue with the authenticated request.
        return chain.proceed(authenticatedRequest)
    }
}