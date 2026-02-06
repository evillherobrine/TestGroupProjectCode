package com.example.musicplayer.data.repository.history

import android.content.Context
import com.example.musicplayer.data.local.AppDatabase
import com.example.musicplayer.data.local.history.HistoryEntry
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class HistoryRepositoryImpl(context: Context) {
    private val historyDao = AppDatabase.getDatabase(context).historyDao()
    fun getHistory(): Flow<List<HistoryEntry>> {
        return historyDao.getHistory()
    }
    suspend fun add(song: Song) {
        val entry = HistoryEntry.fromSong(song)
        historyDao.insert(entry)
        historyDao.trimHistory()
    }
    suspend fun deleteEntry(entryId: Long) {
        historyDao.deleteEntry(entryId)
    }
    suspend fun getMostRecentSongId(): Long? {
        return historyDao.getMostRecentSongId()
    }
    suspend fun clear() {
        historyDao.clearAll()
    }
    suspend fun getRecentSongs(): List<HistoryEntry> {
        return historyDao.getRecentSongs()
    }
    suspend fun getMostPlayedSongs(limit: Int = 50): List<HistoryEntry> {
        return historyDao.getMostPlayedSongs(limit)
    }
    suspend fun getQuickMix(limit: Int = 50): List<HistoryEntry> {
        return historyDao.getQuickMix(limit)
    }
    suspend fun getUserPersona(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis
        val recentHistory = historyDao.getHistoryInPeriod(startTime, endTime)
        if (recentHistory.isEmpty()) {
            return "New Listener" to "🎧"
        }
        var morningCount = 0
        var afternoonCount = 0
        var eveningCount = 0
        var nightCount = 0
        val cal = Calendar.getInstance()
        recentHistory.forEach { entry ->
            cal.timeInMillis = entry.timestamp
            when (cal.get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> morningCount++
                in 12..17 -> afternoonCount++
                in 18..22 -> eveningCount++
                else -> nightCount++
            }
        }
        val maxCategory = mapOf(
            "Early Bird" to morningCount,
            "Daydreamer" to afternoonCount,
            "Night Owl" to eveningCount,
            "After Dark" to nightCount
        ).maxByOrNull { it.value }
        return when (maxCategory?.key) {
            "Early Bird" -> "Early Bird" to "🌅"
            "Daydreamer" -> "Daydreamer" to "☀️"
            "Night Owl" -> "Night Owl" to "🦉"
            else -> "After Dark" to "🌃"
        }
    }
}