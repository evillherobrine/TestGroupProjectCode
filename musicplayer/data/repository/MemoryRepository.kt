package com.example.musicplayer.data.repository

import android.content.Context
import com.example.musicplayer.data.local.AppDatabase
import com.example.musicplayer.data.local.memory.SongMemory
import kotlinx.coroutines.flow.Flow

class MemoryRepository(context: Context) {
    private val memoryDao = AppDatabase.getDatabase(context).memoryDao()
    fun getMemory(songId: Long): Flow<SongMemory?> {
        return memoryDao.getMemoryForSong(songId)
    }
    suspend fun saveMemory(songId: Long, note: String, mood: String) {
        val memory = SongMemory(songId = songId, note = note, mood = mood)
        memoryDao.insertMemory(memory)
    }
    suspend fun deleteMemory(songId: Long) {
        memoryDao.deleteMemory(songId)
    }
}