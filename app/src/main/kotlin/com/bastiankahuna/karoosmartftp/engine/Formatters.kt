package com.bastiankahuna.karoosmartftp.engine

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    fun mmss(ms: Long): String {
        val total = (ms.coerceAtLeast(0L) + 999L) / 1000L
        val minutes = total / 60L
        val seconds = total % 60L
        return "%02d:%02d".format(minutes, seconds)
    }

    fun durationShort(seconds: Int): String {
        val minutes = seconds / 60
        val rem = seconds % 60
        return if (rem == 0) "${minutes}min" else "${minutes}:${rem.toString().padStart(2, '0')}"
    }

    fun clockNow(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    /**
     * Karoo speed stream values are numeric data-point values. The app treats them as m/s and
     * renders km/h, which is the conventional Android/GPS unit conversion. If device logs show
     * Karoo already emits user-unit speed, this is isolated here for one-line correction.
     */
    fun speedKmh(speedMetersPerSecond: Double?): String {
        val speed = speedMetersPerSecond ?: return "---"
        return "%.1f".format(Locale.US, speed * 3.6)
    }
}
