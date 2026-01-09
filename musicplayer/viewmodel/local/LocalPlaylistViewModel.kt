package com.example.musicplayer.viewmodel.local

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.local.AppDatabase
import com.example.musicplayer.data.local.playlist.PlaylistWithSongs
import com.example.musicplayer.data.repository.playlist.UserPlaylistRepository
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocalPlaylistViewModel(
    application: Application,
    private val playlistId: Long
) : AndroidViewModel(application) {
    private val playlistDao = AppDatabase.getDatabase(application).playlistDao()
    private val userPlaylistRepository = UserPlaylistRepository(application)
    val playlist: StateFlow<PlaylistWithSongs?> = playlistDao.getPlaylistWithSongsFlow(playlistId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    private var dbUpdateJob: Job? = null
    private var isReordering = false
    init {
        viewModelScope.launch {
            playlistDao.getPlaylistSongsOrdered(playlistId).collect { entityList ->
                if (!isReordering) {
                    _songs.value = entityList.map { it.toSong() }
                }
            }
        }
    }
    fun renamePlaylist(newName: String) {
        viewModelScope.launch {
            userPlaylistRepository.renamePlaylist(playlistId, newName)
        }
    }
    fun moveSong(fromPosition: Int, toPosition: Int) {
        val currentList = _songs.value.toMutableList()
        if (fromPosition in currentList.indices && toPosition in currentList.indices) {
            isReordering = true
            val item = currentList.removeAt(fromPosition)
            currentList.add(toPosition, item)
            _songs.value = currentList
            dbUpdateJob?.cancel()
            dbUpdateJob = viewModelScope.launch {
                delay(500)
                val newOrderIds = currentList.map { it.id }
                userPlaylistRepository.updatePlaylistOrder(playlistId, newOrderIds)
                delay(200)
                isReordering = false
            }
        }
    }
    fun removeSongs(songIds: List<Long>) {
        viewModelScope.launch {
            songIds.forEach { id ->
                userPlaylistRepository.removeSongFromPlaylist(playlistId, id)
            }
            val currentList = _songs.value.toMutableList()
            currentList.removeAll { it.id in songIds }
            _songs.value = currentList
        }
    }
    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val application: Application,
        private val playlistId: Long
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LocalPlaylistViewModel::class.java)) {
                return LocalPlaylistViewModel(application, playlistId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}