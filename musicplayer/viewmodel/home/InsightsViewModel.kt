package com.example.musicplayer.viewmodel.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.local.AppDatabase
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val historyDao = AppDatabase.getDatabase(application).historyDao()
    val totalTimeText: StateFlow<String> = historyDao.getTotalListeningTime()
        .map { formatDuration(it ?: 0L) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0m")
    val topArtist: StateFlow<String> = historyDao.getTopArtists()
        .map { artistStats ->
            artistStats.firstOrNull()?.artist ?: "N/A"
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, "N/A")
    val topSong: StateFlow<Song?> = historyDao.getMostPlayedSong()
        .map { historyEntry ->
            historyEntry?.toSong()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    private val _weeklyActivity = MutableStateFlow<Map<String, Int>>(emptyMap())
    val weeklyActivity: StateFlow<Map<String, Int>> = _weeklyActivity.asStateFlow()
    private val _userPersona = MutableStateFlow("Music Fan" to "🎵")
    val userPersona: StateFlow<Pair<String, String>> = _userPersona.asStateFlow()
    init {
        calculateWeeklyInsights()
    }
    private fun calculateWeeklyInsights() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val startTime = calendar.timeInMillis
            val recentHistory = historyDao.getHistoryInPeriod(startTime, endTime)
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val activityMap = linkedMapOf<String, Int>()
            for (i in 6 downTo 0) {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                val dayName = dayFormat.format(cal.time)
                activityMap[dayName] = 0
            }
            recentHistory.forEach { entry ->
                val dayName = dayFormat.format(entry.timestamp)
                activityMap[dayName] = (activityMap[dayName] ?: 0) + 1
            }
            _weeklyActivity.value = activityMap
            if (recentHistory.isNotEmpty()) {
                val hourCounts = recentHistory.groupingBy {
                    val c = Calendar.getInstance()
                    c.timeInMillis = it.timestamp
                    c.get(Calendar.HOUR_OF_DAY)
                }.eachCount()
                val maxHour = hourCounts.maxByOrNull { it.value }?.key ?: 12
                val (title, icon) = when (maxHour) {
                    in 5..11 -> "Early Bird" to "🌅"
                    in 12..17 -> "Daydreamer" to "☀️"
                    in 18..22 -> "Night Owl" to "🦉"
                    else -> "After Dark" to "🌃"
                }
                _userPersona.value = title to icon
            }
        }
    }
    private fun formatDuration(millis: Long): String {
        if (millis == 0L) return "0m"
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }
}
