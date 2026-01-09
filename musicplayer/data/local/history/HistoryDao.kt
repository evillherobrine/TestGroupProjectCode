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
    @Query("""
        SELECT * FROM listening_history 
        GROUP BY songId 
        ORDER BY COUNT(songId) DESC 
        LIMIT :limit
    """)
    suspend fun getMostPlayedSongs(limit: Int = 50): List<HistoryEntry>
    @Query("""
        SELECT * FROM listening_history 
        GROUP BY songId 
        ORDER BY RANDOM() 
        LIMIT :limit
    """)
    suspend fun getQuickMix(limit: Int = 50): List<HistoryEntry>
    @Query("SELECT COUNT(DISTINCT songId) FROM listening_history")
    fun getUniqueHistoryCount(): Flow<Int>
    @Query("SELECT artist, COUNT(id) as playCount FROM listening_history GROUP BY artist ORDER BY playCount DESC LIMIT 5")
    fun getTopArtists(): Flow<List<ArtistStat>>
    @Query("SELECT SUM(duration) FROM listening_history")
    fun getTotalListeningTime(): Flow<Long?>
    @Query("""
        SELECT * FROM listening_history 
        GROUP BY songId 
        ORDER BY COUNT(songId) DESC 
        LIMIT 1
    """)
    fun getMostPlayedSong(): Flow<HistoryEntry?>
    @Query("SELECT * FROM listening_history WHERE timestamp BETWEEN :start AND :end")
    suspend fun getHistoryInPeriod(start: Long, end: Long): List<HistoryEntry>
}