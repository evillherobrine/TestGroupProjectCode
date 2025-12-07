package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface QueueRepository {
    val queue: Flow<List<Song>>
    val currentSong: Flow<Song?>
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