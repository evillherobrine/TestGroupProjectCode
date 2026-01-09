package com.example.musicplayer.data.local.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_memories")
data class SongMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val note: String,
    val mood: String = "😊",
    val timestamp: Long = System.currentTimeMillis()
)