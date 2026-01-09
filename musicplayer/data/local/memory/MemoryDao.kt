package com.example.musicplayer.data.local.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM song_memories WHERE songId = :songId ORDER BY timestamp DESC LIMIT 1")
    fun getMemoryForSong(songId: Long): Flow<SongMemory?>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: SongMemory)
    @Query("DELETE FROM song_memories WHERE songId = :songId")
    suspend fun deleteMemory(songId: Long)
}