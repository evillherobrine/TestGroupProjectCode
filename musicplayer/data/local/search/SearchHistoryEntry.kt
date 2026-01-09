package com.example.musicplayer.data.local.search

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "search_queries",
    indices = [Index(value = ["query"], unique = true)]
)
data class SearchHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val timestamp: Long,
    val hasEnoughResults: Boolean
)