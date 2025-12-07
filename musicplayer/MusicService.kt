package com.example.musicplayer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.example.musicplayer.data.repository.FavoriteRepositoryImpl
import com.example.musicplayer.data.repository.HistoryRepositoryImpl
import com.example.musicplayer.data.repository.QueueRepositoryImpl
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.viewmodel.playback.MusicPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicService : MediaBrowserServiceCompat() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val timerHandler = Handler(Looper.getMainLooper())
    private var sleepTimerRunnable: Runnable? = null
    internal lateinit var musicPlayer: MusicPlayer
    internal lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: MusicNotificationManager
    private lateinit var queueRepository: QueueRepositoryImpl
    private lateinit var favoriteRepository: FavoriteRepositoryImpl
    internal var currentTitle: String = ""
    internal var currentArtist: String = ""
    internal var currentUrl: String = ""
    internal var currentCover: String = ""
    internal var coverXL: String = ""
    internal var isCurrentSongFavorite: Boolean = false
    internal var currentSongId: Long = -1L
    private lateinit var historyRepository: HistoryRepositoryImpl
    companion object {
        const val TIMER = "ACTION_SET_SLEEP_TIMER"
        const val EXTRA_TIMER = "EXTRA_TIMER_DURATION_MS"
        const val SEEKBAR_UPDATE = "MUSIC_SEEKBAR_UPDATE"
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
        const val PREFS_NAME = "MusicPlayerPrefs"
        const val KEY_LAST_SONG_TITLE = "LAST_SONG_TITLE"
        const val KEY_LAST_SONG_ARTIST = "LAST_SONG_ARTIST"
        const val KEY_LAST_SONG_URL = "LAST_SONG_URL"
        const val KEY_LAST_SONG_COVER = "LAST_SONG_COVER"
        const val KEY_LAST_SONG_COVER_XL = "LAST_SONG_COVER_XL"
        const val EXTRA_SONG_ID = "EXTRA_SONG_ID"
        const val KEY_LAST_SONG_ID = "LAST_SONG_ID"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        queueRepository = QueueRepositoryImpl
        favoriteRepository = FavoriteRepositoryImpl(applicationContext)
        historyRepository = HistoryRepositoryImpl(applicationContext)
        musicPlayer = MusicPlayer(this)
        notificationManager = MusicNotificationManager(this, musicPlayer)
        notificationManager.createNotificationChannel()
        loadLastSong()

        val mediaSessionCallback = object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                musicPlayer.togglePlayPause()
            }
            override fun onPause() {
                musicPlayer.togglePlayPause()
            }
            override fun onSkipToNext() {
                val next = QueueRepositoryImpl.playNext()
                next?.let { serviceScope.launch { playSong(it) } }
            }
            override fun onSkipToPrevious() {
                val prev = QueueRepositoryImpl.playPrevious()
                prev?.let { serviceScope.launch { playSong(it) } }
            }
            override fun onSeekTo(pos: Long) {
                musicPlayer.seekTo(pos)
            }
            override fun onCustomAction(action: String, extras: Bundle?) {
                when (action) {
                    CUSTOM_ACTION_TOGGLE_FAVORITE -> handleToggleFavorite()
                    else -> super.onCustomAction(action, extras)
                }
            }
        }

        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(mediaSessionCallback)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken
        updatePlaybackState()

        musicPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) handleSongEnded()
                updatePlaybackState()
                updateNotification()
                notificationManager.updateMediaMetadata()
                sendProgressBroadcast()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
                updateNotification()
                sendProgressBroadcast()
                if (!isPlaying) {
                    saveLastSong()
                }
            }
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                updatePlaybackState()
                sendSeekbarUpdate()
            }
        })
        startForeground(notificationManager.notificationId, notificationManager.buildNotification(mediaSession))
        serviceScope.launch {
            while (true) {
                if (::musicPlayer.isInitialized && musicPlayer.isPlaying) {
                    sendSeekbarUpdate()
                }
                delay(1000)
            }
        }
    }
    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                TIMER -> {
                    val durationMs = intent.getLongExtra(EXTRA_TIMER, 0L)
                    setSleepTimer(durationMs)
                }
                REQUEST_UI_UPDATE -> {
                    sendProgressBroadcast()
                }
                TOGGLE_PLAY -> {
                    if (musicPlayer.isReady()) {
                        musicPlayer.togglePlayPause()
                    } else {
                        musicPlayer.play(currentUrl)
                    }
                }
                SEEK_TO -> {
                    val position = intent.getLongExtra("SEEK_TO", 0L)
                    musicPlayer.seekTo(position)
                }
                TOGGLE_REPEAT -> {
                    musicPlayer.toggleRepeatMode()
                    sendProgressBroadcast()
                }
                PLAY_URL -> {
                    val url = intent.getStringExtra("URL") ?: return START_NOT_STICKY
                    currentUrl = url
                    currentTitle = intent.getStringExtra("TITLE") ?: ""
                    currentArtist = intent.getStringExtra("ARTIST") ?: ""
                    currentCover = intent.getStringExtra("COVER") ?: ""
                    coverXL = intent.getStringExtra("COVER_XL") ?: ""
                    currentSongId = intent.getLongExtra(EXTRA_SONG_ID, -1L)
                    serviceScope.launch {
                        if (currentSongId != -1L) {
                            isCurrentSongFavorite = favoriteRepository.isFavorite(currentSongId)
                        }
                        updateNotification()
                        updatePlaybackState()
                    }
                    saveLastSong()
                    notificationManager.loadCoverArt(coverXL)
                    musicPlayer.turnOffRepeatOne()
                    musicPlayer.play(url)
                    val currentSong = QueueRepositoryImpl.currentSong.value
                    currentSong?.let { songToSave ->
                        serviceScope.launch(Dispatchers.IO) {
                            val lastHistorySongId = historyRepository.getMostRecentSongId()
                            if (lastHistorySongId != songToSave.id) {
                                historyRepository.add(songToSave)
                            }
                        }
                    }
                }
                NEXT -> {
                    val next = QueueRepositoryImpl.playNext()
                    next?.let { serviceScope.launch { playSong(it) } }
                }
                PREVIOUS -> {
                    val prev = QueueRepositoryImpl.playPrevious()
                    prev?.let { serviceScope.launch { playSong(it) } }
                }
                CLEAR_QUEUE -> {
                    QueueRepositoryImpl.clearQueue()
                    musicPlayer.stop()
                    sendBroadcast(Intent("QUEUE_CLEARED"))
                }
                STOP -> stopSelf()
                TOGGLE_FAVORITE_NOTIFICATION -> handleToggleFavorite()
            }
        }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        super.onDestroy()
        musicPlayer.release()
        notificationManager.release()
        mediaSession.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceScope.cancel()
    }
    @Suppress("DEPRECATION")
    override fun onTaskRemoved(rootIntent: Intent?) {
        saveLastSong()
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }
    private fun handleSongEnded() {
        serviceScope.launch(Dispatchers.Main) {
            if (musicPlayer.isRepeating) {
                musicPlayer.seekTo(0)
                musicPlayer.togglePlayPause()
            } else {
                val next = withContext(Dispatchers.IO) {
                    queueRepository.playNext()
                }
                if (next != null) {
                    playSong(next)
                } else {
                    musicPlayer.seekTo(0)
                    musicPlayer.pause()
                }
            }
        }
    }
    internal suspend fun playSong(song: Song) {
        val refreshedSong = queueRepository.getPlayableSong(song)
        if (refreshedSong != null) {
            queueRepository.setCurrentSong(refreshedSong)
            currentSongId = refreshedSong.id
            currentUrl = refreshedSong.url
            currentTitle = refreshedSong.title
            currentArtist = refreshedSong.artist
            currentCover = refreshedSong.cover ?: ""
            coverXL = refreshedSong.coverXL ?: ""
            isCurrentSongFavorite = favoriteRepository.isFavorite(refreshedSong.id)
            saveLastSong()
            notificationManager.loadCoverArt(coverXL)
            updateNotification()
            updatePlaybackState()
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val lastHistorySongId = historyRepository.getMostRecentSongId()
                    if (lastHistorySongId != refreshedSong.id) {
                        historyRepository.add(refreshedSong)
                    }
                } catch (e: Exception) {
                    Log.e("MusicService", "Failed to save history", e)
                }
            }
            musicPlayer.turnOffRepeatOne()
            if (refreshedSong.url.isNotEmpty()) {
                musicPlayer.play(refreshedSong.url)
            }
        }
    }
    internal fun sendProgressBroadcast() {
        val isPlaying = musicPlayer.isPlaying
        val isRepeating = musicPlayer.isRepeating
        val intent = Intent("MUSIC_PROGRESS_UPDATE").apply {
            setPackage(this@MusicService.packageName)
            putExtra("isPlaying", isPlaying)
            putExtra("isRepeating", isRepeating)
            putExtra("title", currentTitle)
            putExtra("artist", currentArtist)
            putExtra("cover", currentCover)
            putExtra("cover_xl", coverXL)
            putExtra("isFavorite", isCurrentSongFavorite)
        }
        sendBroadcast(intent)
    }
    private fun setSleepTimer(durationMs: Long) {
        sleepTimerRunnable?.let { timerHandler.removeCallbacks(it) }
        sleepTimerRunnable = null
        when {
            durationMs > 0 -> {
                sleepTimerRunnable = Runnable {
                    if (musicPlayer.isPlaying) {
                        musicPlayer.pause()
                    }
                }
                timerHandler.postDelayed(sleepTimerRunnable!!, durationMs)
            }
        }
    }
    private fun sendSeekbarUpdate() {
        if (::musicPlayer.isInitialized) {
            val duration = if (musicPlayer.duration == C.TIME_UNSET) 0L else musicPlayer.duration
            val position = musicPlayer.currentPosition
            val intent = Intent(SEEKBAR_UPDATE).apply {
                setPackage(this@MusicService.packageName)
                putExtra("position", position)
                putExtra("duration", duration)
            }
            sendBroadcast(intent)
        }
    }
    fun triggerNotificationUpdate() {
        updateNotification()
    }
    private fun updateNotification() {
        if (!::notificationManager.isInitialized || !::mediaSession.isInitialized) return
        notificationManager.updateNotification(mediaSession, isCurrentSongFavorite)
    }
    private fun updatePlaybackState() {
        if (::musicPlayer.isInitialized && ::mediaSession.isInitialized) {
            val position = musicPlayer.currentPosition
            val playbackSpeed = if (musicPlayer.isPlaying) 1f else 0f
            val state = if (musicPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            val playbackStateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_SEEK_TO or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_STOP
                )
                .setState(state, position, playbackSpeed, SystemClock.elapsedRealtime())
                .setBufferedPosition(musicPlayer.bufferedPosition)
            val favoriteIcon = if (isCurrentSongFavorite) R.drawable.favorite_checked else R.drawable.favorite_24px
            val favoriteTitle = if (isCurrentSongFavorite) "Unlike" else "Like"
            playbackStateBuilder.addCustomAction(
                PlaybackStateCompat.CustomAction.Builder(
                    CUSTOM_ACTION_TOGGLE_FAVORITE,
                    favoriteTitle,
                    favoriteIcon
                ).build()
            )
            mediaSession.setPlaybackState(playbackStateBuilder.build())
        }
    }
    private fun saveLastSong() {
        if (currentUrl.isEmpty()) return
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit {
            putString(KEY_LAST_SONG_URL, currentUrl)
            putString(KEY_LAST_SONG_TITLE, currentTitle)
            putString(KEY_LAST_SONG_ARTIST, currentArtist)
            putString(KEY_LAST_SONG_COVER, currentCover)
            putString(KEY_LAST_SONG_COVER_XL, coverXL)
            putLong(KEY_LAST_SONG_ID, currentSongId)
        }
    }
    private fun loadLastSong() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val url = prefs.getString(KEY_LAST_SONG_URL, null)
        if (url != null) {
            currentUrl = url
            currentTitle = prefs.getString(KEY_LAST_SONG_TITLE, "") ?: ""
            currentArtist = prefs.getString(KEY_LAST_SONG_ARTIST, "") ?: ""
            currentCover = prefs.getString(KEY_LAST_SONG_COVER, "") ?: ""
            coverXL = prefs.getString(KEY_LAST_SONG_COVER_XL, "") ?: ""
            currentSongId = prefs.getLong(KEY_LAST_SONG_ID, -1L)
            musicPlayer.prepare(currentUrl)
            notificationManager.loadCoverArt(coverXL)
            serviceScope.launch {
                isCurrentSongFavorite =
                    if (currentSongId != -1L) favoriteRepository.isFavorite(currentSongId) else false
                updateNotification()
                updatePlaybackState()
            }
        }
    }
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot("root", null)
    }
    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.detach()
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
        if (currentUrl.isNotEmpty()) {
            val description = MediaDescriptionCompat.Builder()
                .setTitle(currentTitle)
                .setSubtitle(currentArtist)
                .setIconUri(if (coverXL.isNotEmpty()) coverXL.toUri() else null)
                .setMediaId(currentUrl)
                .build()
            val mediaItem = MediaBrowserCompat.MediaItem(
                description,
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
            mediaItems.add(mediaItem)
        }
        result.sendResult(mediaItems)
    }
    private fun handleToggleFavorite() {
        val song = QueueRepositoryImpl.currentSong.value ?: return
        isCurrentSongFavorite = !isCurrentSongFavorite
        updateNotification()
        updatePlaybackState()
        val intent = Intent("ACTION_FAVORITE_CHANGED").apply {
            putExtra(IS_FAVORITE, isCurrentSongFavorite)
            setPackage(packageName)
        }
        sendBroadcast(intent)
        serviceScope.launch(Dispatchers.IO) {
            favoriteRepository.toggleFavorite(song)
        }
    }
}