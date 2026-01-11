package com.example.musicplayer.viewmodel.playback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.musicplayer.MusicPlayerApp
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@UnstableApi
class QueueViewModel(application: Application) : AndroidViewModel(application) {
    private val queueUseCase = MusicPlayerApp.queueUseCase
    val queue: StateFlow<List<Song>> = queueUseCase.queue
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val currentSong: StateFlow<Song?> = queueUseCase.currentSong
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    fun add(song: Song) {
        viewModelScope.launch { queueUseCase.add(song) }
    }
    fun remove(song: Song) {
        viewModelScope.launch { queueUseCase.remove(song) }
    }
    fun moveSongInQueue(fromPosition: Int, toPosition: Int) {
        viewModelScope.launch { queueUseCase.moveSongInQueue(fromPosition, toPosition) }
    }
    fun shuffle() {
        viewModelScope.launch { queueUseCase.shuffle() }
    }

}
