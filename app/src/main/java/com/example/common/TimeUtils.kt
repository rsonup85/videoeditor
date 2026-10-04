package com.example.common

import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {

    /**
     * Formats milliseconds into mm:ss
     */
    fun formatTimeShort(millis: Long): String {
        val safeMillis = millis.coerceAtLeast(0)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(safeMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(safeMillis) % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    /**
     * Formats milliseconds into mm:ss.SS (minutes:seconds.hundredths) for precision timeline playhead
     */
    fun formatTimeDetailed(millis: Long): String {
        val safeMillis = millis.coerceAtLeast(0)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(safeMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(safeMillis) % 60
        val hundredths = (safeMillis % 1000) / 10
        return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
    }

    /**
     * Formats duration into human readable string (e.g. 1m 24s or 14s)
     */
    fun formatDurationHuman(millis: Long): String {
        val safeMillis = millis.coerceAtLeast(0)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(safeMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(safeMillis) % 60
        return if (minutes > 0) {
            "${minutes}m ${seconds}s"
        } else {
            "${seconds}s"
        }
    }
}
