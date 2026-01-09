package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    val favoriteSongs: Flow<List<Song>>
    suspend fun isFavorite(songId: Long): Boolean
    suspend fun toggleFavorite(song: Song)
}
