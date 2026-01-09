package com.example.musicplayer.viewmodel.memory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.MusicPlayerApp
import com.example.musicplayer.data.local.memory.SongMemory
import com.example.musicplayer.data.repository.MemoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MemoryRepository(application)
    private val queueUseCase = MusicPlayerApp.queueUseCase
    val currentMemory: StateFlow<SongMemory?> = queueUseCase.currentSong
        .flatMapLatest { song ->
            if (song != null) {
                repository.getMemory(song.id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    fun saveMemory(note: String, mood: String) {
        val currentSongId = queueUseCase.currentSong.value?.id ?: return
        viewModelScope.launch {
            repository.saveMemory(currentSongId, note, mood)
        }
    }
    fun deleteMemory() {
        val currentSongId = queueUseCase.currentSong.value?.id ?: return
        viewModelScope.launch {
            repository.deleteMemory(currentSongId)
        }
    }
}