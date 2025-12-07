package com.example.musicplayer.viewmodel.playback

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.MusicService
import com.example.musicplayer.data.repository.QueueRepositoryImpl
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.data.repository.FavoriteRepositoryImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableLiveData(PlayerUiState())
    val uiState: LiveData<PlayerUiState> = _uiState
    private val favoriteRepository = FavoriteRepositoryImpl(application)
    private val queueRepository = QueueRepositoryImpl
    private var playJob: Job? = null
    private var timerJob: Job? = null
    private val musicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                _uiState.postValue(_uiState.value?.copy(
                    isPlaying = it.getBooleanExtra("isPlaying", false),
                    isRepeating = it.getBooleanExtra("isRepeating", false),
                    title = it.getStringExtra("title") ?: "",
                    artist = it.getStringExtra("artist") ?: "",
                    coverUrl = it.getStringExtra("cover") ?: "",
                    coverUrlXL = it.getStringExtra("cover_xl") ?: "",
                    isFavourite = it.getBooleanExtra("isFavorite", false)
                ))
            }
        }
    }
    private val seekbarReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val position = it.getLongExtra("position", 0L)
                val duration = it.getLongExtra("duration", 0L)
                _uiState.postValue(_uiState.value?.copy(
                    position = position,
                    duration = duration
                ))
            }
        }
    }
    private val favoriteToggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "ACTION_FAVORITE_CHANGED") {
                val isFav = intent.getBooleanExtra(MusicService.IS_FAVORITE, false)
                _uiState.postValue(_uiState.value?.copy(isFavourite = isFav))
            }
        }
    }
    init {
        viewModelScope.launch {
            combine(
                queueRepository.currentSong,
                queueRepository.queue,
                favoriteRepository.favoriteSongs
            ) { currentSong, queue, favoriteList ->
                val isFavorite = currentSong?.let { song ->
                    favoriteList.any { it.id == song.id }
                } ?: false
                val currentIndex = if (currentSong != null) {
                    queue.indexOfFirst { it.id == currentSong.id }
                } else -1
                val nextSongTitle = if (currentIndex != -1 && currentIndex + 1 < queue.size) {
                    queue[currentIndex + 1].title
                } else {
                    ""
                }
                PlayerData(currentSong, isFavorite, nextSongTitle, queue, currentIndex)
            }.collect { data ->
                if (data.currentSong != null) {
                    val currentState = _uiState.value ?: PlayerUiState()
                    _uiState.postValue(currentState.copy(
                        currentSong = data.currentSong,
                        isFavourite = data.isFavorite,
                        upNextSong = data.nextSongTitle,
                        queue = data.queue,
                        currentIndex = data.currentIndex
                    ))
                }
            }
        }
    }
    private val receivers = listOf(
        musicReceiver to IntentFilter("MUSIC_PROGRESS_UPDATE"),
        seekbarReceiver to IntentFilter(MusicService.SEEKBAR_UPDATE),
        favoriteToggleReceiver to IntentFilter("ACTION_FAVORITE_CHANGED")
    )
    fun registerReceivers() {
        val context = getApplication<Application>()
        receivers.forEach { (receiver, filter) ->
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }
    fun unregisterReceivers() {
        val context = getApplication<Application>()
        receivers.forEach { (receiver, _) ->
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) { }
        }
    }
    private fun sendMusicCommand(action: String, extras: Map<String, Any> = emptyMap()) {
        val context = getApplication<Application>()
        val intent = Intent(context, MusicService::class.java).apply {
            this.action = action
            extras.forEach { (key, value) ->
                when (value) {
                    is Long -> putExtra(key, value)
                    is String -> putExtra(key, value)
                }
            }
        }
        context.startForegroundService(intent)
    }
    fun playSong(song: Song) {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            try {
                val songCopy = song.copy()
                val playableSong = queueRepository.getPlayableSong(songCopy)
                if (playableSong != null && playableSong.url.isNotEmpty()) {
                    queueRepository.add(playableSong)
                    queueRepository.setCurrentSong(playableSong)
                    sendMusicCommand("PLAY_URL",
                        mapOf(
                            "URL" to playableSong.url,
                            "TITLE" to playableSong.title,
                            "ARTIST" to playableSong.artist,
                            "COVER" to (playableSong.cover ?: ""),
                            "COVER_XL" to (playableSong.coverXL ?: ""),
                            MusicService.EXTRA_SONG_ID to playableSong.id
                        )
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
            }
        }
    }
    fun playSongList(clickedSong: Song, songList: List<Song>) {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            try {
                val songCopy = clickedSong.copy()
                val playableSong = queueRepository.getPlayableSong(songCopy) ?: return@launch
                queueRepository.clearQueue()
                val songsToSave = songList.map { song ->
                    if (song.id == playableSong.id) playableSong else song
                }
                songsToSave.forEach { queueRepository.add(it) }
                queueRepository.setCurrentSong(playableSong)
                sendMusicCommand("PLAY_URL",
                    mapOf(
                        "URL" to playableSong.url,
                        "TITLE" to playableSong.title,
                        "ARTIST" to playableSong.artist,
                        "COVER" to (playableSong.cover ?: ""),
                        "COVER_XL" to (playableSong.coverXL ?: ""),
                        MusicService.EXTRA_SONG_ID to playableSong.id
                    )
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
            }
        }
    }
    fun togglePlayPause() = sendMusicCommand(MusicService.TOGGLE_PLAY)
    fun toggleRepeat() = sendMusicCommand(MusicService.TOGGLE_REPEAT)
    fun toggleFavorite() = sendMusicCommand(MusicService.TOGGLE_FAVORITE_NOTIFICATION)
    fun nextSong() {
        val nextSong = queueRepository.playNext()
        if (nextSong != null) {
            playSong(nextSong)
        }
    }
    fun prevSong() {
        val prevSong = queueRepository.playPrevious()
        if (prevSong != null) {
            playSong(prevSong)
        }
    }
    fun seekTo(position: Long) {
        _uiState.value = _uiState.value?.copy(position = position)
        sendMusicCommand(MusicService.SEEK_TO, mapOf("SEEK_TO" to position))
    }
    fun setSleepTimer(durationMs: Long) {
        sendMusicCommand(MusicService.TIMER, mapOf(MusicService.EXTRA_TIMER to durationMs))
        startUiCountdown(durationMs)
    }
    fun addSleepTimerMinutes(minutes: Int) {
        val currentRemaining = _uiState.value?.sleepTimerInMillis ?: 0L
        val newDuration = currentRemaining + (minutes * 60 * 1000L)
        setSleepTimer(newDuration)
    }
    private fun startUiCountdown(durationMs: Long) {
        timerJob?.cancel()

        if (durationMs <= 0) {
            _uiState.value = _uiState.value?.copy(sleepTimerInMillis = null)
            return
        }
        timerJob = viewModelScope.launch {
            var remaining = durationMs
            val startTime = System.currentTimeMillis()
            val endTime = startTime + durationMs

            while (isActive && remaining > 0) {
                _uiState.postValue(_uiState.value?.copy(sleepTimerInMillis = remaining))
                delay(1000)
                remaining = endTime - System.currentTimeMillis()
            }
            _uiState.postValue(_uiState.value?.copy(sleepTimerInMillis = null))
        }
    }
    fun requestInitialState() = sendMusicCommand(MusicService.REQUEST_UI_UPDATE)
    override fun onCleared() {
        super.onCleared()
        unregisterReceivers()
    }
}
data class PlayerData(
    val currentSong: Song?,
    val isFavorite: Boolean,
    val nextSongTitle: String,
    val queue: List<Song>,
    val currentIndex: Int
)