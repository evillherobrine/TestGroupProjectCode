package com.example.musicplayer.data.local.queue

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue ORDER BY queueOrder ASC")
    fun getQueueFlow(): Flow<List<QueueEntry>>
    @Upsert
    suspend fun upsertAll(entries: List<QueueEntry>)
    @Query("DELETE FROM queue")
    suspend fun clearQueue()
}