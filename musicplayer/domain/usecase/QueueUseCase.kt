package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.QueueRepository
import kotlinx.coroutines.flow.StateFlow

class QueueUseCase(private val queueRepository: QueueRepository) {
    val queue: StateFlow<List<Song>> = queueRepository.queue
    val currentSong: StateFlow<Song?> = queueRepository.currentSong
    fun add(song: Song): Boolean = queueRepository.add(song)
    fun remove(song: Song) = queueRepository.remove(song)
    fun moveSongInQueue(fromPosition: Int, toPosition: Int) {
        queueRepository.moveSongInQueue(fromPosition, toPosition)
    }
    fun shuffle() = queueRepository.shuffle()
    fun clearQueue() = queueRepository.clearQueue()
    fun playNext(): Song? = queueRepository.playNext()
    fun playPrevious(): Song? = queueRepository.playPrevious()
    fun setCurrentSong(song: Song?) = queueRepository.setCurrentSong(song)
    suspend fun getPlayableSong(song: Song): Song? = queueRepository.getPlayableSong(song)
}