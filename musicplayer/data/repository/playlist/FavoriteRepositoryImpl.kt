package com.example.musicplayer.data.repository.playlist

import android.content.Context
import com.example.musicplayer.data.local.AppDatabase
import com.example.musicplayer.data.local.playlist.toFavouriteSong
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRepositoryImpl(context: Context) : FavoriteRepository {
    private val favoriteDao = AppDatabase.getDatabase(context).favouriteDao()
    override val favoriteSongs: Flow<List<Song>> = favoriteDao.getFavoritesFlow().map { list ->
        list.map { it.toSong() }
    }
    override suspend fun isFavorite(songId: Long): Boolean {
        return favoriteDao.isFavorite(songId)
    }
    override suspend fun toggleFavorite(song: Song) {
        val isCurrentlyFav = isFavorite(song.id)
        if (isCurrentlyFav) {
            favoriteDao.updateFavoriteStatus(song.id, false)
        } else {
            val existingSong = favoriteDao.getById(song.id)
            if (existingSong != null) {
                favoriteDao.updateFavoriteStatus(song.id, true)
            } else {
                val newFav = song.toFavouriteSong().copy(isFavorite = true)
                favoriteDao.insert(newFav)
            }
        }
    }
}