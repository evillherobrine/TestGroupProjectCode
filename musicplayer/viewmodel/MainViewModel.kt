package com.example.musicplayer.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.musicplayer.domain.model.Song

class MainViewModel : ViewModel() {
    var showQueueSheet by mutableStateOf(false)
        private set
    var showSleepTimerSheet by mutableStateOf(false)
        private set
    var showSleepTimerDialog by mutableStateOf(false)
        private set
    var showSongOptionsSheet by mutableStateOf(false)
        private set
    var selectedSong: Song? = null
        private set
    var selectedHistoryId: Long? = null
        private set
    var selectedPlaylistId: Long? = null
        private set
    var onSelectionCallback: (() -> Unit)? = null
        private set
    var homeScrollTrigger by mutableLongStateOf(0L)
        private set
    var libraryScrollTrigger by mutableLongStateOf(0L)
        private set
    var localScrollTrigger by mutableLongStateOf(0L)
        private set
    fun openQueue() { showQueueSheet = true }
    fun closeQueue() { showQueueSheet = false }
    fun openSleepTimer(isTimerRunning: Boolean) {
        if (isTimerRunning) showSleepTimerSheet = true else showSleepTimerDialog = true
    }
    fun closeSleepTimerSheet() { showSleepTimerSheet = false }
    fun closeSleepTimerDialog() { showSleepTimerDialog = false }
    fun openSongOptions(
        song: Song,
        historyId: Long? = null,
        playlistId: Long? = null,
        callback: (() -> Unit)? = null
    ) {
        selectedSong = song
        selectedHistoryId = historyId
        selectedPlaylistId = playlistId
        onSelectionCallback = callback
        showSongOptionsSheet = true
    }
    fun closeSongOptions() {
        showSongOptionsSheet = false
        selectedSong = null
        selectedHistoryId = null
        selectedPlaylistId = null
        onSelectionCallback = null
    }
    fun scrollToTopHome() { homeScrollTrigger = System.currentTimeMillis() }
    fun scrollToTopLibrary() { libraryScrollTrigger = System.currentTimeMillis() }
    fun scrollToTopLocal() { localScrollTrigger = System.currentTimeMillis() }
}