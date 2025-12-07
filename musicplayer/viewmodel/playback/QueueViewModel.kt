package com.example.musicplayer.viewmodel.playback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.repository.QueueRepositoryImpl
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.usecase.QueueUseCase
import kotlinx.coroutines.launch

class QueueViewModel(application: Application) : AndroidViewModel(application) {
    private val queueRepository = QueueRepositoryImpl
    private val queueUseCase = QueueUseCase(queueRepository)
    val queue = queueUseCase.queue.asLiveData()
    val currentSong = queueUseCase.currentSong.asLiveData()
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
