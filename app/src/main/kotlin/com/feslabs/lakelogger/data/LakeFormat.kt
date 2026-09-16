package com.feslabs.lakelogger.data

import java.util.Locale
import kotlin.math.roundToInt

/** Formatting helpers shared by the Compose UI and the Glance widget. */
object LakeFormat {
    fun meters(value: Double?, fractionDigits: Int = 3): String {
        if (value == null) return "—"
        return String.format(Locale.US, "%.${fractionDigits}f m", value)
    }

    fun celsius(value: Double?, fractionDigits: Int = 1): String {
        if (value == null) return "—"
        return String.format(Locale.US, "%.${fractionDigits}f°C", value)
    }

    fun percent(value: Double?, fractionDigits: Int = 0): String {
        if (value == null) return "—"
        return String.format(Locale.US, "%.${fractionDigits}f%%", value)
    }

    fun volts(value: Double?, fractionDigits: Int = 2): String {
        if (value == null) return "—"
        return String.format(Locale.US, "%.${fractionDigits}fV", value)
    }

    fun amps(value: Double?, fractionDigits: Int = 2): String {
        if (value == null) return "—"
        return String.format(Locale.US, "%.${fractionDigits}fA", value)
    }

    fun windSpeed(metersPerSecond: Double?): String {
        if (metersPerSecond == null) return "—"
        return String.format(Locale.US, "%.1f m/s", metersPerSecond)
    }

    fun humidity(value: Double?): String = percent(value, 0)

    fun pressure(hpa: Double?): String {
        if (hpa == null) return "—"
        return String.format(Locale.US, "%.0f hPa", hpa)
    }

    fun rainfall(mm: Double?): String {
        if (mm == null) return "—"
        return String.format(Locale.US, "%.1f mm", mm)
    }

    private val compassDirections = listOf(
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
    )

    fun compassDirection(degrees: Double?): String {
        if (degrees == null) return "—"
        val normalized = ((degrees % 360) + 360) % 360
        val index = (normalized / 22.5).roundToInt() % 16
        return compassDirections[index]
    }

    /** A short, human-friendly "how long ago" string (e.g. "5m ago", "2h ago"). */
    fun relativeTime(epochMillis: Long?, nowEpochMillis: Long = System.currentTimeMillis()): String {
        if (epochMillis == null) return "never"
        val seconds = maxOf(0L, (nowEpochMillis - epochMillis) / 1000)

        return when {
            seconds < 60 -> "just now"
            seconds < 3600 -> "${seconds / 60}m ago"
            seconds < 86_400 -> "${seconds / 3600}h ago"
            else -> "${seconds / 86_400}d ago"
        }
    }

    /** A short duration string for a raw seconds count (e.g. device uptime). */
    fun relativeDuration(totalSeconds: Long?): String {
        if (totalSeconds == null || totalSeconds < 0) return "—"
        return when {
            totalSeconds < 60 -> "${totalSeconds}s"
            totalSeconds < 3600 -> "${totalSeconds / 60}m"
            totalSeconds < 86_400 -> "${totalSeconds / 3600}h ${(totalSeconds % 3600) / 60}m"
            else -> "${totalSeconds / 86_400}d ${(totalSeconds % 86_400) / 3600}h"
        }
    }
}
