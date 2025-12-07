package com.example.musicplayer.viewmodel.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.repository.LocalAudioRepository
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalMusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalAudioRepository(application)
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()
    fun updatePermissionStatus(granted: Boolean) {
        _hasPermission.value = granted
        if (granted) {
            loadLocalMusic()
        } else {
            _isLoading.value = false
        }
    }
    private fun loadLocalMusic() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                repository.getLocalAudio()
            }
            _songs.value = result
            _isLoading.value = false
        }
    }
}