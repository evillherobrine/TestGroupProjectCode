package com.example.musicplayer.viewmodel.playback

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.musicplayer.MusicPlayerApp
import com.example.musicplayer.data.repository.playlist.FavoriteRepositoryImpl
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicStateRepository
import com.example.musicplayer.service.MusicService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
@UnstableApi
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val favoriteRepository = FavoriteRepositoryImpl(application)
    private val queueUseCase = MusicPlayerApp.queueUseCase
    private var playJob: Job? = null
    private var timerJob: Job?  = null
    private val _sleepTimerInMillis = MutableStateFlow<Long?>(null)
    private val _isLoading = MutableStateFlow(false)

    private data class PlayerMetadata(
        val isPlaying: Boolean,
        val isRepeating: Boolean,
        val currentSong: Song?,
        val queue: List<Song>,
        val isFavorite: Boolean,
        val currentIndex: Int,
        val upNextSong: String,
        val isNightModeEnabled:  Boolean
    )

    private val metadataFlow = combine(
        MusicStateRepository.isPlaying,
        MusicStateRepository.isRepeating,
        queueUseCase.currentSong,
        queueUseCase.queue,
        favoriteRepository.favoriteSongs,
        MusicStateRepository.isNightMode
    ) { values ->
        val isPlaying = values[0] as Boolean
        val isRepeating = values[1] as Boolean
        val currentSong = values[2] as Song?
        @Suppress("UNCHECKED_CAST")
        val queue = values[3] as List<Song>
        @Suppress("UNCHECKED_CAST")
        val favorites = values[4] as List<Song>
        val isNightMode = values[5] as Boolean

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
        PlayerMetadata(
            isPlaying = isPlaying,
            isRepeating = isRepeating,
            currentSong = currentSong,
            queue = queue,
            isFavorite = isFavorite,
            currentIndex = currentIndex,
            upNextSong = nextSongTitle,
            isNightModeEnabled = isNightMode
        )
    }

    private val progressFlow = combine(
        MusicStateRepository.currentPosition,
        MusicStateRepository.duration
    ) { position, duration ->
        Pair(position, duration)
    }

    val uiState: StateFlow<PlayerUiState> = combine(
        metadataFlow,
        progressFlow,
        _sleepTimerInMillis,
        _isLoading
    ) { metadata, progress, sleepTimer, isLoading ->
        PlayerUiState(
            isPlaying = metadata.isPlaying,
            isRepeating = metadata.isRepeating,
            isLoading = isLoading,
            title = metadata.currentSong?.title ?: "",
            artist = metadata.currentSong?.artist ?:  "",
            coverUrl = metadata.currentSong?.cover ?: "",
            coverUrlXL = metadata.currentSong?.coverXL ?: "",
            position = progress.first,
            duration = progress.second,
            isFavourite = metadata.isFavorite,
            upNextSong = metadata.upNextSong,
            currentSong = metadata.currentSong,
            queue = metadata.queue,
            currentIndex = metadata.currentIndex,
            sleepTimerInMillis = sleepTimer,
            isNightModeEnabled = metadata.isNightModeEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerUiState()
    )

    private fun sendMusicCommand(action: String, extras:  Map<String, Any> = emptyMap()) {
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

    fun playSong(song:  Song) {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                if (MusicStateRepository.isPlaying.value) {
                    sendMusicCommand(MusicService.TOGGLE_PLAY)
                }
                val songCopy = song.copy()
                val playableSong = queueUseCase.getPlayableSong(songCopy)
                if (playableSong != null && playableSong.url.isNotEmpty()) {
                    queueUseCase.add(playableSong)
                    queueUseCase.setCurrentSong(playableSong)
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
            } catch (e:  Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playSongList(clickedSong: Song, songList: List<Song>) {
        playJob?.cancel()
        playJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                if (MusicStateRepository.isPlaying.value) {
                    sendMusicCommand(MusicService.TOGGLE_PLAY)
                }
                val songCopy = clickedSong.copy()
                val playableSong = queueUseCase.getPlayableSong(songCopy) ?: return@launch
                queueUseCase.clearQueue()
                val songsToSave = songList.map { song ->
                    if (song.id == playableSong.id) playableSong else song
                }
                songsToSave.forEach { queueUseCase.add(it) }
                queueUseCase.setCurrentSong(playableSong)
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
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun togglePlayPause() = sendMusicCommand(MusicService.TOGGLE_PLAY)
    fun toggleRepeat() = sendMusicCommand(MusicService.TOGGLE_REPEAT)
    fun toggleFavorite() = sendMusicCommand(MusicService.TOGGLE_FAVORITE_NOTIFICATION)

    fun nextSong() {
        val nextSong = queueUseCase.playNext()
        if (nextSong != null) playSong(nextSong)
    }

    fun prevSong() {
        val prevSong = queueUseCase.playPrevious()
        if (prevSong != null) playSong(prevSong)
    }

    fun seekTo(position: Long) {
        sendMusicCommand(MusicService.SEEK_TO, mapOf("SEEK_TO" to position))
    }

    fun setSleepTimer(durationMs: Long) {
        sendMusicCommand(MusicService.TIMER, mapOf(MusicService.EXTRA_TIMER to durationMs))
        startUiCountdown(durationMs)
    }

    fun addSleepTimerMinutes(minutes: Int) {
        val currentRemaining = _sleepTimerInMillis.value ?: 0L
        val newDuration = currentRemaining + (minutes * 60 * 1000L)
        setSleepTimer(newDuration)
    }

    private fun startUiCountdown(durationMs: Long) {
        timerJob?.cancel()
        if (durationMs <= 0) {
            _sleepTimerInMillis.value = null
            return
        }
        timerJob = viewModelScope.launch {
            val endTime = System.currentTimeMillis() + durationMs
            while (isActive) {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    _sleepTimerInMillis.value = null
                    break
                }
                _sleepTimerInMillis.value = remaining
                delay(1000)
            }
        }
    }
    fun toggleNightMode() {
        sendMusicCommand(MusicService.TOGGLE_NIGHT_MODE)
    }
    fun requestInitialState() = sendMusicCommand(MusicService.REQUEST_UI_UPDATE)
}