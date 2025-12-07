package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.QueueRepository
import kotlinx.coroutines.flow.Flow

class QueueUseCase(private val queueRepository: QueueRepository) {

    val queue: Flow<List<Song>> = queueRepository.queue
    val currentSong: Flow<Song?> = queueRepository.currentSong
    suspend fun add(song: Song): Boolean = queueRepository.add(song)
    suspend fun remove(song: Song) = queueRepository.remove(song)
    suspend fun moveSongInQueue(fromPosition: Int, toPosition: Int) = queueRepository.moveSongInQueue(fromPosition, toPosition)
    suspend fun shuffle() = queueRepository.shuffle()
}
