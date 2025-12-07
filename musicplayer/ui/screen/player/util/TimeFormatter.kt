package com.example.musicplayer.ui.screen.player

import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatTime(milliseconds: Long): String {
    if (milliseconds < 0L) return "--:--"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}