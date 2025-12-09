package com.example.musicplayer.service

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Runnable

class SleepTimerManager(private val onTimerExpired: () -> Unit) {
    private val timerHandler = Handler(Looper.getMainLooper())
    private var sleepTimerRunnable: Runnable? = null
    fun setTimer(durationMs: Long) {
        cancelTimer()
        if (durationMs > 0) {
            sleepTimerRunnable = Runnable {
                onTimerExpired()
            }
            timerHandler.postDelayed(sleepTimerRunnable!!, durationMs)
        }
    }
    fun cancelTimer() {
        sleepTimerRunnable?.let { timerHandler.removeCallbacks(it) }
        sleepTimerRunnable = null
    }
}