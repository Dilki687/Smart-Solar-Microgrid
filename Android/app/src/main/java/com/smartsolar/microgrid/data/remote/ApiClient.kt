package com.smartsolar.microgrid.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Provides the Retrofit client used by the Android application
 * to communicate with the central C# Web API.
 */
object ApiClient {

    /*
     * Android Emulator uses 10.0.2.2 to access the host computer.
     *
     * The C# API is running on:
     *
     * http://localhost:5147
     */
    private const val BASE_URL = "http://10.0.2.2:5147/"

    private lateinit var retrofit: Retrofit

    private lateinit var authApiInstance: AuthApi

    private lateinit var prosumerApiInstance: ProsumerApi

    /**
     * Initializes the Retrofit client.
     *
     * This must be called once when the application starts.
     */
    fun initialize(context: Context) {

        // HTTP logging for development.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        /*
         * Build OkHttp client.
         *
         * AuthInterceptor automatically adds:
         *
         * Authorization: Bearer <JWT>
         */
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(
                AuthInterceptor(context.applicationContext)
            )
            .addInterceptor(loggingInterceptor)
            .build()

        // Build Retrofit.
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()

        // Create API services.
        authApiInstance =
            retrofit.create(AuthApi::class.java)

        prosumerApiInstance =
            retrofit.create(ProsumerApi::class.java)
    }

    /**
     * Authentication API.
     */
    val authApi: AuthApi
        get() = authApiInstance

    /**
     * Prosumer API.
     */
    val prosumerApi: ProsumerApi
        get() = prosumerApiInstance
}