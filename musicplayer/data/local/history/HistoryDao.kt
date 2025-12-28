package com.example.musicplayer.data.local.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entry: HistoryEntry)
    @Query("SELECT * FROM listening_history ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<HistoryEntry>>
    @Query("DELETE FROM listening_history WHERE id NOT IN (SELECT id FROM listening_history ORDER BY timestamp DESC LIMIT 200)")
    suspend fun trimHistory()
    @Query("DELETE FROM listening_history WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)
    @Query("SELECT songId FROM listening_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentSongId(): Long?
    @Query("DELETE FROM listening_history")
    suspend fun clearAll()
    @Query("""
        SELECT * FROM listening_history 
        WHERE id IN (
            SELECT MAX(id) 
            FROM listening_history 
            GROUP BY songId
        ) 
        ORDER BY timestamp DESC 
        LIMIT 20
    """)
    suspend fun getRecentSongs(): List<HistoryEntry>
}