package com.example.musicplayer.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "song")
data class Song(
    @PrimaryKey val id: Long,
    val title: String,
    val url: String,
    val artist: String,
    val cover: String? = "",
    val coverXL: String? = "",
    val duration: Long = 0L,
    val lastFetchTime: Long = 0,
    val isLocal: Boolean = false
): Serializable