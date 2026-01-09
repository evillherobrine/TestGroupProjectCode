package com.example.musicplayer.service

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.OptIn
import androidx.core.app.ServiceCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.example.musicplayer.MusicNotificationManager
import com.example.musicplayer.MusicPlayerApp
import com.example.musicplayer.data.repository.history.HistoryRepositoryImpl
import com.example.musicplayer.data.repository.playlist.FavoriteRepositoryImpl
import com.example.musicplayer.domain.model.RepeatMode
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicStateRepository
import com.example.musicplayer.viewmodel.playback.MusicPlayer
import kotlinx.coroutines.*

@UnstableApi
class MusicService : MediaBrowserServiceCompat() {
    companion object {
        const val TIMER = "ACTION_SET_SLEEP_TIMER"
        const val EXTRA_TIMER = "EXTRA_TIMER_DURATION_MS"
        const val REQUEST_UI_UPDATE = "ACTION_REQUEST_UI_UPDATE"
        const val PLAY_URL = "PLAY_URL"
        const val TOGGLE_PLAY = "TOGGLE_PLAY"
        const val SEEK_TO = "SEEK_TO"
        const val TOGGLE_REPEAT = "TOGGLE_REPEAT"
        const val NEXT = "NEXT"
        const val PREVIOUS = "PREVIOUS"
        const val CLEAR_QUEUE = "CLEAR_QUEUE"
        const val STOP = "STOP"
        const val TOGGLE_FAVORITE_NOTIFICATION = "TOGGLE_FAVORITE_FROM_NOTIFICATION"
        const val IS_FAVORITE = "EXTRA_IS_FAVORITE"
        const val CUSTOM_ACTION_TOGGLE_FAVORITE = "CUSTOM_ACTION_TOGGLE_FAVORITE"
        const val EXTRA_SONG_ID = "EXTRA_SONG_ID"
        const val EXTRA_IS_LOCAL = "EXTRA_IS_LOCAL"
        const val ACTION_APP_FOREGROUND = "ACTION_APP_FOREGROUND"
        const val ACTION_APP_BACKGROUND = "ACTION_APP_BACKGROUND"
        const val TOGGLE_NIGHT_MODE = "TOGGLE_NIGHT_MODE"
        const val SET_NIGHT_MODE = "SET_NIGHT_MODE"
        const val EXTRA_NIGHT_MODE_ENABLED = "EXTRA_NIGHT_MODE_ENABLED"
    }

    private val serviceJob = SupervisorJob()
    private var isAppInForeground = true
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    internal lateinit var musicPlayer: MusicPlayer
    private lateinit var notificationManager: MusicNotificationManager
    private lateinit var mediaSessionHandler: MediaSessionHandler
    private lateinit var sleepTimerManager: SleepTimerManager
    private lateinit var cacheManager: MusicCacheManager
    private lateinit var playerPreferences: PlayerPreferences
    private lateinit var favoriteRepository: FavoriteRepositoryImpl
    private lateinit var historyRepository: HistoryRepositoryImpl
    internal var currentTitle: String = ""
    internal var currentArtist: String = ""
    internal var isCurrentSongFavorite: Boolean = false
    private var seekbarJob: Job? = null
    private var loadSongJob: Job? = null
    private var targetSongId: Long = -1L
    internal var currentSongId: Long = -1L
    internal lateinit var mediaSession: MediaSessionCompat

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        musicPlayer = MusicPlayer(this)
        favoriteRepository = FavoriteRepositoryImpl(applicationContext)
        historyRepository = HistoryRepositoryImpl(applicationContext)
        cacheManager = MusicCacheManager(applicationContext)
        playerPreferences = PlayerPreferences(applicationContext)
        sleepTimerManager = SleepTimerManager { if (musicPlayer.isPlaying) musicPlayer.pause() }
        mediaSessionHandler = MediaSessionHandler(this, musicPlayer) { action, extras ->
            handleAction(action, extras)
        }
        mediaSession = mediaSessionHandler.mediaSession
        sessionToken = mediaSession.sessionToken
        notificationManager = MusicNotificationManager(this, musicPlayer)
        notificationManager.createNotificationChannel()
        restoreLastSession()
        setupPlayerListeners()
        ServiceCompat.startForeground(
            this,
            notificationManager.notificationId,
            notificationManager.buildNotification(mediaSession),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        )
    }

    private fun handleAction(action: String, extras: Bundle?) {
        when (action) {
            TOGGLE_PLAY -> handleTogglePlay()
            NEXT -> handleNext()
            PREVIOUS -> handlePrevious()
            SEEK_TO -> musicPlayer.seekTo(extras?.getLong("SEEK_TO") ?: 0L)
            CUSTOM_ACTION_TOGGLE_FAVORITE -> handleToggleFavorite()
        }
    }

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        when (action) {
            TIMER -> sleepTimerManager.setTimer(intent.getLongExtra(EXTRA_TIMER, 0L))
            REQUEST_UI_UPDATE -> {
                sendProgressBroadcast()
                MusicStateRepository.setNightMode(musicPlayer.isNightModeEnabled)
            }
            TOGGLE_PLAY -> handleTogglePlay()
            SEEK_TO -> musicPlayer.seekTo(intent.getLongExtra("SEEK_TO", 0L))
            TOGGLE_REPEAT -> {
                musicPlayer.toggleRepeatMode()
                sendProgressBroadcast()
            }
            PLAY_URL -> handlePlayUrlIntent(intent)
            NEXT -> handleNext()
            PREVIOUS -> handlePrevious()
            CLEAR_QUEUE -> {
                MusicPlayerApp.queueUseCase.clearQueue()
                musicPlayer.stop()
                sendBroadcast(Intent("QUEUE_CLEARED"))
            }
            STOP -> stopSelf()
            TOGGLE_FAVORITE_NOTIFICATION -> handleToggleFavorite()
            ACTION_APP_FOREGROUND -> {
                isAppInForeground = true
                if (musicPlayer.isPlaying) startSeekbarUpdates()
            }
            ACTION_APP_BACKGROUND -> {
                isAppInForeground = false
                stopSeekbarUpdates()
            }
            TOGGLE_NIGHT_MODE -> {
                android.util.Log.d("MusicService", "Nhận lệnh TOGGLE_NIGHT_MODE")
                val newState = musicPlayer.toggleNightMode()
                MusicStateRepository.setNightMode(newState)
            }
            SET_NIGHT_MODE -> {
                val enabled = intent.getBooleanExtra(EXTRA_NIGHT_MODE_ENABLED, false)
                musicPlayer.setNightModeEnabled(enabled)
                MusicStateRepository.setNightMode(enabled)
            }
        }
        return START_NOT_STICKY
    }
    private fun setupPlayerListeners() {
        musicPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) handleSongEnded()
                updateUIAndNotification()
                if (state == Player.STATE_READY && musicPlayer.isPlaying) {
                    startSeekbarUpdates()
                } else if (state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                    stopSeekbarUpdates()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateUIAndNotification()
                if (isPlaying) startSeekbarUpdates()
                else {
                    stopSeekbarUpdates()
                    saveCurrentState()
                }
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                mediaSessionHandler.updatePlaybackState(isCurrentSongFavorite)
            }
        })
    }
    internal fun playSong(song: Song) {
        targetSongId = song.id
        loadSongJob?.cancel()
        musicPlayer.stop()
        musicPlayer.clearMediaItems()
        MusicPlayerApp.queueUseCase.setCurrentSong(song)
        currentSongId = song.id
        currentTitle = song.title
        currentArtist = song.artist
        triggerNotificationUpdate()
        mediaSessionHandler.updatePlaybackState(isCurrentSongFavorite)
        loadSongJob = serviceScope.launch {
            try {
                val refreshedSong = withContext(Dispatchers.IO) {
                    MusicPlayerApp.queueUseCase.getPlayableSong(song)
                }
                if (refreshedSong == null || refreshedSong.id != targetSongId || !isActive) return@launch
                val currentUrl = refreshedSong.url
                saveCurrentState(refreshedSong)
                notificationManager.loadCoverArt(refreshedSong.coverXL ?: "")
                triggerNotificationUpdate()
                launch(Dispatchers.IO) {
                    try {
                        if (historyRepository.getMostRecentSongId() != refreshedSong.id) {
                            historyRepository.add(refreshedSong)
                        }
                    } catch (_: Exception) {
                    }
                }
                musicPlayer.turnOffRepeatOne()
                if (currentUrl.isNotEmpty()) {
                    val cacheKey = if (refreshedSong.isLocal) null else refreshedSong.id.toString()
                    musicPlayer.play(currentUrl, cacheKey)
                    cacheManager.prefetchQueueWindow(this)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
            }
        }
    }

    private fun handlePlayUrlIntent(intent: Intent) {
        val url = intent.getStringExtra("URL") ?: return
        val song = Song(
            id = intent.getLongExtra(EXTRA_SONG_ID, -1L),
            title = intent.getStringExtra("TITLE") ?: "",
            artist = intent.getStringExtra("ARTIST") ?: "",
            url = url,
            cover = intent.getStringExtra("COVER") ?: "",
            coverXL = intent.getStringExtra("COVER_XL") ?: "",
            isLocal = intent.getBooleanExtra(EXTRA_IS_LOCAL, false)
        )
        serviceScope.launch { playSong(song) }
    }

    private fun handleTogglePlay() {
        if (musicPlayer.isReady()) {
            musicPlayer.togglePlayPause()
        } else {
            playerPreferences.loadLastSong()?.let { lastSong ->
                if (lastSong.url.isNotEmpty()) {
                    musicPlayer.prepare(lastSong.url)
                    musicPlayer.togglePlayPause()
                }
            }
        }
    }
    private fun handleNext() {
        val next = MusicPlayerApp.queueUseCase.playNext()
        next?.let { serviceScope.launch { playSong(it) } }
    }
    private fun handlePrevious() {
        val prev = MusicPlayerApp.queueUseCase.playPrevious()
        prev?.let { serviceScope.launch { playSong(it) } }
    }
    private fun handleSongEnded() {
        serviceScope.launch(Dispatchers.Main) {
            val currentMode = musicPlayer.repeatMode
            if (currentMode == RepeatMode.ONE) {
                val currentSong = MusicPlayerApp.queueUseCase.currentSong.value
                if (currentSong != null) {
                    launch(Dispatchers.IO) {
                        historyRepository.add(currentSong)
                    }
                }
                musicPlayer.seekTo(0)
                musicPlayer.play()
            } else {
                val next = withContext(Dispatchers.IO) {
                    MusicPlayerApp.queueUseCase.playNext()
                }
                if (next != null) {
                    playSong(next)
                } else {
                    if (currentMode == RepeatMode.ALL) {
                        val firstSong = MusicPlayerApp.queueUseCase.queue.value.firstOrNull()
                        if (firstSong != null) {
                            playSong(firstSong)
                        }
                    } else {
                        musicPlayer.seekTo(0)
                        musicPlayer.pause()
                    }
                }
            }
        }
    }
    private fun handleToggleFavorite() {
        val song = MusicPlayerApp.queueUseCase.currentSong.value ?: return
        isCurrentSongFavorite = !isCurrentSongFavorite
        updateUIAndNotification()
        sendBroadcast(Intent("ACTION_FAVORITE_CHANGED").apply {
            putExtra(IS_FAVORITE, isCurrentSongFavorite)
            setPackage(packageName)
        })
        serviceScope.launch(Dispatchers.IO) { favoriteRepository.toggleFavorite(song) }
    }
    internal fun triggerNotificationUpdate() {
        if (!::notificationManager.isInitialized) return
        notificationManager.updateNotification(mediaSession, isCurrentSongFavorite)
    }
    private fun updateUIAndNotification() {
        triggerNotificationUpdate()
        mediaSessionHandler.updatePlaybackState(isCurrentSongFavorite)
        notificationManager.updateMediaMetadata()
        sendProgressBroadcast()
    }
    internal fun sendProgressBroadcast() {
        MusicStateRepository.updatePlaybackState(musicPlayer.isPlaying, musicPlayer.repeatMode)
    }
    private fun startSeekbarUpdates() {
        if (seekbarJob?.isActive == true) return
        if (!isAppInForeground) return
        seekbarJob = serviceScope.launch(Dispatchers.Main) {
            while (isActive && ::musicPlayer.isInitialized && musicPlayer.isPlaying) {
                MusicStateRepository.updateProgress(musicPlayer.currentPosition, musicPlayer.duration.coerceAtLeast(0L))
                delay(1000)
            }
        }
    }
    private fun stopSeekbarUpdates() {
        seekbarJob?.cancel()
        seekbarJob = null
    }
    private fun saveCurrentState(song: Song? = null) {
        val current = song ?: MusicPlayerApp.queueUseCase.currentSong.value ?: return
        playerPreferences.saveLastSong(current)
    }
    private fun restoreLastSession() {
        val lastSong = playerPreferences.loadLastSong()
        if (lastSong != null && lastSong.url.isNotEmpty()) {
            currentTitle = lastSong.title
            currentArtist = lastSong.artist
            currentSongId = lastSong.id
            musicPlayer.prepare(lastSong.url)
            notificationManager.loadCoverArt(lastSong.coverXL ?: "")
            serviceScope.launch {
                isCurrentSongFavorite = if (currentSongId != -1L) favoriteRepository.isFavorite(currentSongId) else false
                updateUIAndNotification()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSeekbarUpdates()
        musicPlayer.release()
        notificationManager.release()
        mediaSessionHandler.release()
        sleepTimerManager.cancelTimer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceScope.cancel()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        saveCurrentState()
        stopSelf()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(mutableListOf())
    }
}
