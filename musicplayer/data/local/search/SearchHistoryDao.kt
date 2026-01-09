package com.example.musicplayer.data.local.search

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SearchHistoryEntry)
    @Query("SELECT * FROM search_queries ORDER BY timestamp DESC LIMIT 50")
    fun searchHistoryFlow(): Flow<List<SearchHistoryEntry>>
    @Query("DELETE FROM search_queries WHERE id = :id")
    suspend fun deleteById(id: Long)
    @Query("DELETE FROM search_queries")
    suspend fun clearAll()
    @Query("DELETE FROM search_queries WHERE `query` = :query")
    suspend fun keywordDelete(query: String)
    @Query("SELECT * FROM search_queries WHERE hasEnoughResults = 1 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomEntries(limit: Int): List<SearchHistoryEntry>
    @Query("SELECT * FROM search_queries WHERE hasEnoughResults = 1 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatestEntries(limit: Int): List<SearchHistoryEntry>
}