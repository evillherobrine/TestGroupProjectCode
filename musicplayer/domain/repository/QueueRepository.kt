package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.StateFlow

interface QueueRepository {
    val queue: StateFlow<List<Song>>
    val currentSong: StateFlow<Song?>
    fun add(song: Song): Boolean
    fun remove(song: Song)
    fun playNext(): Song?
    fun playPrevious(): Song?
    fun setCurrentSong(song: Song?)
    fun clearQueue()
    fun getNextSong(): Song?
    fun moveSongInQueue(fromPosition: Int, toPosition: Int)
    fun shuffle()
    suspend fun getPlayableSong(song: Song): Song?
}