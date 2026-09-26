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
     * The C# API runs on http://localhost:5147 on the developer's
     * PC. The Android app talks to it through an adb port-forward
     * that maps the phone's local port 5147 to the developer PC's:
     *
     *   adb reverse tcp:5147 tcp:5147
     *
     * The Gradle install task in app/build.gradle.kts re-applies
     * this mapping automatically every time you run installDebug,
     * so you never have to run it by hand.
     *
     * Works for a physical phone over USB and the AVD emulator.
     */
    private val BASE_URL = com.smartsolar.microgrid.BuildConfig.API_BASE_URL

    private lateinit var retrofit: Retrofit

    private lateinit var authApiInstance: AuthApi

    private lateinit var prosumerApiInstance: ProsumerApi

    private lateinit var reservationApiInstance: ReservationApi

    private lateinit var bookingSlotApiInstance: BookingSlotApi

    private lateinit var stationApiInstance: StationApi

    private lateinit var userApiInstance: UserApi
    private lateinit var operatorApiInstance: OperatorApi
    val operatorApi: OperatorApi
        get() = operatorApiInstance

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
        operatorApiInstance = retrofit.create(OperatorApi::class.java)
        authApiInstance =
            retrofit.create(AuthApi::class.java)

        prosumerApiInstance =
            retrofit.create(ProsumerApi::class.java)

        reservationApiInstance =
            retrofit.create(ReservationApi::class.java)

        bookingSlotApiInstance =
            retrofit.create(BookingSlotApi::class.java)

        stationApiInstance =
            retrofit.create(StationApi::class.java)

        userApiInstance =
            retrofit.create(UserApi::class.java)
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

    /**
     * Reservation API.
     */
    val reservationApi: ReservationApi
        get() = reservationApiInstance

    /**
     * Booking slot API.
     */
    val bookingSlotApi: BookingSlotApi
        get() = bookingSlotApiInstance

    /**
     * Station API.
     */
    val stationApi: StationApi
        get() = stationApiInstance

    /**
     * Backoffice user management API.
     */
    val userApi: UserApi
        get() = userApiInstance
}
