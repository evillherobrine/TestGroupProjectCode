package com.example.musicplayer.viewmodel.playback

import com.example.musicplayer.domain.model.RepeatMode
import com.example.musicplayer.domain.model.Song

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isLoading: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val coverUrl: String = "",
    val coverUrlXL: String = "",
    val position: Long = 0L,
    val duration: Long = 0L,
    val isFavourite: Boolean = false,
    val upNextSong: String = "",
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val sleepTimerInMillis: Long? = null,
    val isNightModeEnabled: Boolean = false
)
