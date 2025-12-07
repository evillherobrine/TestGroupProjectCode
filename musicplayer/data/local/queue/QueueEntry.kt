package com.example.musicplayer.data.local.queue

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.musicplayer.domain.model.Song

@Entity(tableName = "queue")
data class QueueEntry(
    @PrimaryKey
    val songId: Long,
    val queueOrder: Int,
    val title: String,
    val artist: String,
    val cover: String?,
    val duration: Long,
    val url: String,
    val lastFetchTime: Long,
    val isLocal: Boolean = false
) {
    fun toSong(): Song {
        return Song(
            id = this.songId,
            title = this.title,
            url = this.url,
            artist = this.artist,
            cover = this.cover,
            coverXL = this.cover?.replace("-large.jpg", "-t500x500.jpg"),
            duration = this.duration,
            lastFetchTime = this.lastFetchTime,
            isLocal = this.isLocal
        )
    }
}