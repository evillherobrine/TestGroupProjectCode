package com.example.musicplayer.data.local.history

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.musicplayer.domain.model.Song

@Entity(tableName="listening_history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val timestamp: Long,
    val title: String,
    val artist: String,
    val cover: String?,
    val duration: Long,
    val isLocal: Boolean = false
) {
    companion object {
        fun fromSong(song: Song): HistoryEntry {
            return HistoryEntry(
                songId = song.id,
                timestamp = System.currentTimeMillis(),
                title = song.title,
                artist = song.artist,
                cover = song.cover,
                duration = song.duration,
                isLocal = song.isLocal
            )
        }
    }
    fun toSong(): Song {
        return Song(
            id = this.songId,
            title = this.title,
            url = "",
            artist = this.artist,
            cover = this.cover,
            coverXL = this.cover?.replace("-large.jpg", "-t500x500.jpg"),
            duration = this.duration,
            lastFetchTime = 0L,
            isLocal = this.isLocal
        )
    }
}