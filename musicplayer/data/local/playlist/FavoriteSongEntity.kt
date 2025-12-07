package com.example.musicplayer.data.local.playlist

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.musicplayer.domain.model.Song

@Entity(tableName = "favourite_songs")
data class FavouriteSong(
    @PrimaryKey val id: Long,
    val title: String?,
    val artist: String?,
    val cover: String?,
    val coverXL: String?,
    val url: String?,
    val isFavorite: Boolean = true,
    val duration: Long?,
    val isLocal: Boolean = false
) {
    fun toSong(): Song {
        return Song(
            id = id,
            title = title ?: "",
            artist = artist ?: "",
            cover = cover ?: "",
            coverXL = coverXL ?: "",
            url = url ?: "",
            duration = duration ?: 0L,
            isLocal = isLocal
        )
    }

}
fun Song.toFavouriteSong(): FavouriteSong {
    return FavouriteSong(
        id = id,
        title = title,
        artist = artist,
        cover = cover,
        coverXL = coverXL,
        url = url,
        duration = duration,
        isLocal = isLocal
    )
}