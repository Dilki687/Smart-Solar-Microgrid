package com.smartsolar.microgrid.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Small formatting helpers used across the reservation UIs so
 * every screen prints dates and status labels the same way.
 */
object ReservationFormat {

    private val incomingIso = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
    )

    private val output = SimpleDateFormat(
        "dd/MM/yyyy, HH:mm", Locale.getDefault(),
    )

    /** Parses an incoming ISO timestamp into a device-local string. */
    fun format(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"

        for (pattern in incomingIso) {
            val fmt = SimpleDateFormat(pattern, Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            try {
                return output.format(fmt.parse(iso) ?: continue)
            } catch (_: ParseException) {
            }
        }
        return iso
    }

    /** Parses an ISO timestamp into a Date, or null on failure. */
    fun parse(iso: String?): Date? {
        if (iso.isNullOrBlank()) return null
        for (pattern in incomingIso) {
            val fmt = SimpleDateFormat(pattern, Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            try {
                return fmt.parse(iso)
            } catch (_: ParseException) {
            }
        }
        return null
    }
}
