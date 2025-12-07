package com.example.musicplayer.viewmodel.playback

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.musicplayer.MusicService
import com.example.musicplayer.data.repository.FavoriteRepositoryImpl
import com.example.musicplayer.data.repository.QueueRepositoryImpl
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicStateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
@UnstableApi
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableLiveData(PlayerUiState())
    val uiState: LiveData<PlayerUiState> = _uiState
    private val favoriteRepository = FavoriteRepositoryImpl(application)
    private var playJob: Job? = null
    private var timerJob: Job? = null
    private data class PlayerStatus(
        val isPlaying: Boolean,
        val isRepeating: Boolean,
        val position: Long,
        val duration: Long
    )

    init {
        viewModelScope.launch {
            val playerStatusFlow = combine(
                MusicStateRepository.isPlaying,
                MusicStateRepository.isRepeating,
                MusicStateRepository.currentPosition,
                MusicStateRepository.duration
            ) { isPlaying, isRepeating, position, duration ->
                PlayerStatus(isPlaying, isRepeating, position, duration)
            }
            combine(
                playerStatusFlow,
                QueueRepositoryImpl.currentSong,
                QueueRepositoryImpl.queue,
                favoriteRepository.favoriteSongs
            ) { status, currentSong, queue, favorites ->
                val (isPlaying, isRepeating, position, duration) = status
                val isFavorite = currentSong?.let { song ->
                    favorites.any { it.id == song.id }
                } ?: false
                val currentIndex = if (currentSong != null) {
                    queue.indexOfFirst { it.id == currentSong.id }
                } else -1
                val nextSongTitle = if (currentIndex != -1 && currentIndex + 1 < queue.size) {
                    queue[currentIndex + 1].title
                } else {
                    ""
                }
                PlayerUiState(
                    isPlaying = isPlaying,
                    isRepeating = isRepeating,
                    title = currentSong?.title ?: "",
                    artist = currentSong?.artist ?: "",
                    coverUrl = currentSong?.cover ?: "",
                    coverUrlXL = currentSong?.coverXL ?: "",
                    position = position,
                    duration = duration,
                    isFavourite = isFavorite,
                    upNextSong = nextSongTitle,
                    currentSong = currentSong,
                    queue = queue,
                    currentIndex = currentIndex,
                    sleepTimerInMillis = _uiState.value?.sleepTimerInMillis
                )
            }.collect { newState ->
                _uiState.postValue(newState)
            }
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
                    is Boolean -> putExtra(key, value)
                }
            }
        }
        context.startService(intent)
    }

    fun playSong(song: Song) {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            try {
                val songCopy = song.copy()
                val playableSong = QueueRepositoryImpl.getPlayableSong(songCopy)
                if (playableSong != null && playableSong.url.isNotEmpty()) {
                    QueueRepositoryImpl.add(playableSong)
                    QueueRepositoryImpl.setCurrentSong(playableSong)
                    sendMusicCommand(MusicService.PLAY_URL, mapOf(
                        "URL" to playableSong.url,
                        "TITLE" to playableSong.title,
                        "ARTIST" to playableSong.artist,
                        "COVER" to (playableSong.cover ?: ""),
                        "COVER_XL" to (playableSong.coverXL ?: ""),
                        MusicService.EXTRA_SONG_ID to playableSong.id,
                        MusicService.EXTRA_IS_LOCAL to playableSong.isLocal
                    ))
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
                val playableSong = QueueRepositoryImpl.getPlayableSong(songCopy) ?: return@launch
                QueueRepositoryImpl.clearQueue()
                val songsToSave = songList.map { song ->
                    if (song.id == playableSong.id) playableSong else song
                }
                songsToSave.forEach { QueueRepositoryImpl.add(it) }
                QueueRepositoryImpl.setCurrentSong(playableSong)
                sendMusicCommand(MusicService.PLAY_URL, mapOf(
                    "URL" to playableSong.url,
                    "TITLE" to playableSong.title,
                    "ARTIST" to playableSong.artist,
                    "COVER" to (playableSong.cover ?: ""),
                    "COVER_XL" to (playableSong.coverXL ?: ""),
                    MusicService.EXTRA_SONG_ID to playableSong.id,
                    MusicService.EXTRA_IS_LOCAL to playableSong.isLocal
                ))
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
        val nextSong = QueueRepositoryImpl.playNext()
        if (nextSong != null) playSong(nextSong)
    }
    fun prevSong() {
        val prevSong = QueueRepositoryImpl.playPrevious()
        if (prevSong != null) playSong(prevSong)
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
            val endTime = System.currentTimeMillis() + durationMs
            while (isActive) {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    _uiState.postValue(_uiState.value?.copy(sleepTimerInMillis = null))
                    break
                }
                _uiState.postValue(_uiState.value?.copy(sleepTimerInMillis = remaining))
                delay(1000)
            }
        }
    }
    fun requestInitialState() = sendMusicCommand(MusicService.REQUEST_UI_UPDATE)
}