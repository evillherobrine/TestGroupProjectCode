package com.example.musicplayer.viewmodel.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.api.RetrofitClient
import com.example.musicplayer.data.local.AppDatabase
import com.example.musicplayer.data.repository.playlist.FavoriteRepositoryImpl
import com.example.musicplayer.data.repository.playlist.UserPlaylistRepository
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.model.toSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val favoriteRepository = FavoriteRepositoryImpl(application)
    private val playlistDao = AppDatabase.getDatabase(application).playlistDao()
    private val userPlaylistRepository = UserPlaylistRepository(application)
    private val favoritesPlaylistFlow = favoriteRepository.favoriteSongs
        .map { favoriteSongs ->
            Playlist(
                id = Playlist.FAVOURITES_PLAYLIST_ID,
                name = "Favorites",
                songCount = favoriteSongs.size,
                coverUrl = favoriteSongs.firstOrNull()?.cover
            )
        }
    private val userPlaylistsFlow = playlistDao.getAllPlaylistsWithSongsFlow()
        .map { playlistsWithSongs ->
            playlistsWithSongs.map {
                Playlist(
                    id = it.playlist.playlistId,
                    name = it.playlist.name,
                    songCount = it.songs.size,
                    coverUrl = it.songs.firstOrNull()?.cover
                )
            }
        }
    val playlists: StateFlow<List<Playlist>> = combine(
        favoritesPlaylistFlow,
        userPlaylistsFlow
    ) { favorites, userPlaylists ->
        listOf(favorites) + userPlaylists
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            userPlaylistRepository.createPlaylist(name)
        }
    }
    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            userPlaylistRepository.renamePlaylist(playlistId, newName)
        }
    }
    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            userPlaylistRepository.addSongToPlaylist(playlistId, song)
        }
    }
    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            userPlaylistRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }
    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            userPlaylistRepository.deletePlaylist(playlistId)
        }
    }
    fun deletePlaylists(playlistIds: List<Long>) {
        viewModelScope.launch {
            playlistIds.forEach { id ->
                if (id != Playlist.FAVOURITES_PLAYLIST_ID && id != -2L) {
                    userPlaylistRepository.deletePlaylist(id)
                }
            }
        }
    }
    fun getPlaylistSongs(playlistId: Long, onResult: (List<Song>) -> Unit) {
        viewModelScope.launch {
            if (playlistId == Playlist.FAVOURITES_PLAYLIST_ID) {
                val songs = favoriteRepository.favoriteSongs.first()
                onResult(songs)
            } else {
                val songs = userPlaylistRepository.getSongsInPlaylist(playlistId)
                onResult(songs)
            }
        }
    }
    fun fetchOnlineSongs(playlistId: Long, onResult: (List<Song>) -> Unit) {
        viewModelScope.launch {
            try {
                val tracks = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getPlaylistTracks(playlistId)
                }
                val songs = tracks.map { it.toSong() }
                onResult(songs)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(emptyList())
            }
        }
    }
    fun importOnlinePlaylist(playlistId: Long, playlistName: String) {
        viewModelScope.launch {
            try {
                val tracks = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getPlaylistTracks(playlistId)
                }
                val songs = tracks.map { it.toSong() }
                if (songs.isNotEmpty()) {
                    userPlaylistRepository.saveOnlinePlaylist(playlistName, songs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
                return LibraryViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}