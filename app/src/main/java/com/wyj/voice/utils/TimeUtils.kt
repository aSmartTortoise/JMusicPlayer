package com.wyj.voice.utils

import android.annotation.SuppressLint

object TimeUtils {
    /**
     * Parse the time in milliseconds into String with the format: hh:mm:ss or mm:ss
     *
     * @param duration The time needs to be parsed.
     */

    @SuppressLint("DefaultLocale")
    fun formatDuration(duration: Int): String? {
        var duration = duration
        duration /= 1000 // milliseconds into seconds
        var minute = duration / 60
        val hour = minute / 60
        minute %= 60
        val second = duration % 60
        return if (hour != 0) String.format(
            "%2d:%02d:%02d",
            hour,
            minute,
            second
        ) else String.format("%02d:%02d", minute, second)
    }
}