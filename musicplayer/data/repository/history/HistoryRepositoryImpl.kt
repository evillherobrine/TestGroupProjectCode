package com.example.musicplayer.data.repository.history

import android.content.Context
import com.example.musicplayer.data.local.AppDatabase
import com.example.musicplayer.data.local.history.HistoryEntry
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

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
}