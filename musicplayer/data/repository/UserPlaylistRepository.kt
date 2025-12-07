package com.example.musicplayer.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.musicplayer.data.local.AppDatabase
import com.example.musicplayer.data.local.playlist.FavouriteSong
import com.example.musicplayer.data.local.playlist.PlaylistSongCrossRef
import com.example.musicplayer.data.local.playlist.UserPlaylist
import com.example.musicplayer.data.local.playlist.toFavouriteSong
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.first

class UserPlaylistRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val playlistDao = db.playlistDao()
    private val favouriteDao = db.favouriteDao()
    suspend fun createPlaylist(name: String) {
        if (playlistDao.getPlaylistByName(name) == null) {
            playlistDao.insertPlaylist(UserPlaylist(name = name))
        }
    }
    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        playlistDao.updatePlaylistName(playlistId, newName)
    }
    suspend fun addSongToPlaylist(playlistId: Long, song: Song) {
        val existingSong = favouriteDao.getById(song.id)
        if (existingSong == null) {
            val localSong = FavouriteSong(
                id = song.id,
                title = song.title,
                artist = song.artist,
                cover = song.cover,
                coverXL = song.coverXL,
                url = song.url,
                duration = song.duration,
                isFavorite = false
            )
            favouriteDao.insert(localSong)
        }
        val maxOrder = playlistDao.getMaxOrderIndex(playlistId) ?: -1
        val newOrder = maxOrder + 1
        playlistDao.insertPlaylistSongCrossRef(
            PlaylistSongCrossRef(playlistId = playlistId, id = song.id, orderIndex = newOrder)
        )
    }
    suspend fun updatePlaylistOrder(playlistId: Long, songIds: List<Long>) {
        db.withTransaction {
            val currentRefs = playlistDao.getCrossRefs(playlistId)
            val refsMap = currentRefs.associateBy { it.id }
            val updatedRefs = songIds.mapIndexedNotNull { index, songId ->
                refsMap[songId]?.copy(orderIndex = index)
            }
            if (updatedRefs.isNotEmpty()) {
                playlistDao.updateCrossRefs(updatedRefs)
            }
        }
    }
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.deleteSongFromPlaylist(PlaylistSongCrossRef(playlistId, songId, 0))
    }
    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylistById(playlistId)
    }
    suspend fun getSongsInPlaylist(playlistId: Long): List<Song> {
        return playlistDao.getPlaylistSongsOrdered(playlistId).first().map { it.toSong() }
    }
    suspend fun saveOnlinePlaylist(name: String, songs: List<Song>) {
        val newPlaylistId = playlistDao.insertPlaylist(UserPlaylist(name = name))
        songs.forEachIndexed { index, song ->
            val localSong = song.toFavouriteSong().copy(isFavorite = false)
            playlistDao.insertSongIfNotExists(localSong)
            playlistDao.insertPlaylistSongCrossRef(
                PlaylistSongCrossRef(playlistId = newPlaylistId, id = song.id, orderIndex = index)
            )
        }
    }

}