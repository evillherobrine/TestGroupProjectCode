package com.example.musicplayer.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val coverUrl: String?
) {
    companion object {
        const val FAVOURITES_PLAYLIST_ID = -1L
        const val MOST_PLAYED_PLAYLIST_ID = -100L
        const val QUICK_MIX_PLAYLIST_ID = -101L
    }
}