package com.smartsolar.microgrid

import android.app.Application
import com.smartsolar.microgrid.data.remote.ApiClient

/**
 * Application class for the Smart Solar Microgrid mobile app.
 *
 * Initializes application-wide services when the app starts.
 */
class SmartSolarApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the central API client.
        ApiClient.initialize(applicationContext)
    }
}