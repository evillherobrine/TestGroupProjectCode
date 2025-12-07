package com.example.musicplayer

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.OptIn
import androidx.core.app.ServiceCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import com.example.musicplayer.cache.MusicCache
import com.example.musicplayer.data.repository.FavoriteRepositoryImpl
import com.example.musicplayer.data.repository.HistoryRepositoryImpl
import com.example.musicplayer.data.repository.QueueRepositoryImpl
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicStateRepository
import com.example.musicplayer.viewmodel.playback.MusicPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
class MusicService : MediaBrowserServiceCompat() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var seekbarJob: Job? = null
    internal lateinit var musicPlayer: MusicPlayer
    internal lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: MusicNotificationManager
    private lateinit var favoriteRepository: FavoriteRepositoryImpl
    private lateinit var historyRepository: HistoryRepositoryImpl
    internal var currentTitle: String = ""
    internal var currentArtist: String = ""
    internal var currentUrl: String = ""
    internal var currentCover: String = ""
    internal var coverXL: String = ""
    internal var isCurrentSongFavorite: Boolean = false
    internal var currentSongId: Long = -1L
    private val timerHandler = Handler(Looper.getMainLooper())
    private var sleepTimerRunnable: Runnable? = null
    private var loadSongJob: Job? = null
    private var targetSongId: Long = -1L
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
        const val KEY_LAST_SONG_ID = "LAST_SONG_ID"
        const val EXTRA_SONG_ID = "EXTRA_SONG_ID"
        const val EXTRA_IS_LOCAL = "EXTRA_IS_LOCAL"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        favoriteRepository = FavoriteRepositoryImpl(applicationContext)
        historyRepository = HistoryRepositoryImpl(applicationContext)
        musicPlayer = MusicPlayer(this)
        notificationManager = MusicNotificationManager(this, musicPlayer)
        notificationManager.createNotificationChannel()
        setupMediaSession()
        loadLastSong()
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
    private fun setupMediaSession() {
        val mediaSessionCallback = object : MediaSessionCompat.Callback() {
            override fun onPlay() = musicPlayer.togglePlayPause()
            override fun onPause() = musicPlayer.togglePlayPause()
            override fun onSkipToNext() = handleNext()
            override fun onSkipToPrevious() = handlePrevious()
            override fun onSeekTo(pos: Long) = musicPlayer.seekTo(pos)
            override fun onCustomAction(action: String, extras: Bundle?) {
                if (action == CUSTOM_ACTION_TOGGLE_FAVORITE) handleToggleFavorite()
            }
        }
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(mediaSessionCallback)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken
    }
    private fun setupPlayerListeners() {
        musicPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) handleSongEnded()
                updatePlaybackState()
                updateNotification()
                notificationManager.updateMediaMetadata()
                sendProgressBroadcast()
                if (state == Player.STATE_READY && musicPlayer.isPlaying) {
                    startSeekbarUpdates()
                } else if (state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                    stopSeekbarUpdates()
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
                updateNotification()
                sendProgressBroadcast()
                if (isPlaying) {
                    startSeekbarUpdates()
                } else {
                    stopSeekbarUpdates()
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
    }
    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        when (action) {
            TIMER -> handleSleepTimer(intent.getLongExtra(EXTRA_TIMER, 0L))
            REQUEST_UI_UPDATE -> sendProgressBroadcast()
            TOGGLE_PLAY -> handleTogglePlay()
            SEEK_TO -> musicPlayer.seekTo(intent.getLongExtra("SEEK_TO", 0L))
            TOGGLE_REPEAT -> {
                musicPlayer.toggleRepeatMode()
                sendProgressBroadcast()
            }
            PLAY_URL -> handlePlayUrlIntent(intent)
            NEXT -> handleNext()
            PREVIOUS -> handlePrevious()
            CLEAR_QUEUE -> handleClearQueue()
            STOP -> stopSelf()
            TOGGLE_FAVORITE_NOTIFICATION -> handleToggleFavorite()
        }
        return START_NOT_STICKY
    }
    private fun handleSleepTimer(durationMs: Long) {
        sleepTimerRunnable?.let { timerHandler.removeCallbacks(it) }
        sleepTimerRunnable = null
        if (durationMs > 0) {
            sleepTimerRunnable = Runnable {
                if (musicPlayer.isPlaying) musicPlayer.pause()
            }
            timerHandler.postDelayed(sleepTimerRunnable!!, durationMs)
        }
    }
    private fun handleTogglePlay() {
        if (musicPlayer.isReady()) {
            musicPlayer.togglePlayPause()
        } else if (currentUrl.isNotEmpty()) {
            musicPlayer.prepare(currentUrl)
            musicPlayer.exoPlayer.play()
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
    private fun handleNext() {
        val next = QueueRepositoryImpl.playNext()
        next?.let { serviceScope.launch { playSong(it) } }
    }
    private fun handlePrevious() {
        val prev = QueueRepositoryImpl.playPrevious()
        prev?.let { serviceScope.launch { playSong(it) } }
    }
    private fun handleClearQueue() {
        QueueRepositoryImpl.clearQueue()
        musicPlayer.stop()
        sendBroadcast(Intent("QUEUE_CLEARED"))
    }
    internal fun playSong(song: Song) {
        targetSongId = song.id
        loadSongJob?.cancel()
        musicPlayer.stop()
        musicPlayer.clearMediaItems()
        QueueRepositoryImpl.setCurrentSong(song)
        currentSongId = song.id
        currentTitle = song.title
        currentArtist = song.artist
        currentCover = song.cover ?: ""
        coverXL = song.coverXL ?: ""
        triggerNotificationUpdate()
        updatePlaybackState()
        loadSongJob = serviceScope.launch {
            try {
                val refreshedSong = withContext(Dispatchers.IO) {
                    QueueRepositoryImpl.getPlayableSong(song)
                }
                if (refreshedSong == null || refreshedSong.id != targetSongId || !isActive) {
                    return@launch
                }
                currentUrl = refreshedSong.url
                saveLastSong()
                notificationManager.loadCoverArt(coverXL)
                triggerNotificationUpdate()
                launch(Dispatchers.IO) {
                    try {
                        val lastHistorySongId = historyRepository.getMostRecentSongId()
                        if (lastHistorySongId != refreshedSong.id) {
                            historyRepository.add(refreshedSong)
                        }
                    } catch (_: Exception) { }
                }
                musicPlayer.turnOffRepeatOne()
                if (currentUrl.isNotEmpty()) {
                    val cacheKey = if (refreshedSong.isLocal) null else refreshedSong.id.toString()
                    musicPlayer.play(currentUrl, cacheKey)
                    prefetchQueueWindow()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.printStackTrace()
            }
        }
    }
    internal fun triggerNotificationUpdate() {
        updateNotification()
    }
    private fun handleSongEnded() {
        serviceScope.launch(Dispatchers.Main) {
            if (musicPlayer.isRepeating) {
                musicPlayer.seekTo(0)
                musicPlayer.togglePlayPause()
            } else {
                val next = withContext(Dispatchers.IO) { QueueRepositoryImpl.playNext() }
                if (next != null) {
                    playSong(next)
                } else {
                    musicPlayer.seekTo(0)
                    musicPlayer.pause()
                }
            }
        }
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
    private fun startSeekbarUpdates() {
        if (seekbarJob?.isActive == true) return
        seekbarJob = serviceScope.launch(Dispatchers.Main) {
            while (isActive && ::musicPlayer.isInitialized && musicPlayer.isPlaying) {
                MusicStateRepository.updateProgress(
                    musicPlayer.currentPosition,
                    musicPlayer.duration.coerceAtLeast(0L)
                )
                delay(1000)
            }
        }
    }
    private fun stopSeekbarUpdates() {
        seekbarJob?.cancel()
        seekbarJob = null
    }
    private fun sendSeekbarUpdate() {
        if (!::musicPlayer.isInitialized) return
        val duration = if (musicPlayer.duration == C.TIME_UNSET) 0L else musicPlayer.duration
        val position = musicPlayer.currentPosition
        val intent = Intent(SEEKBAR_UPDATE).apply {
            setPackage(this@MusicService.packageName)
            putExtra("position", position)
            putExtra("duration", duration)
        }
        sendBroadcast(intent)
    }
    internal fun sendProgressBroadcast() {
        MusicStateRepository.updatePlaybackState(musicPlayer.isPlaying,musicPlayer.isRepeating)
    }
    private fun updateNotification() {
        if (!::notificationManager.isInitialized || !::mediaSession.isInitialized) return
        notificationManager.updateNotification(mediaSession, isCurrentSongFavorite)
    }
    private fun updatePlaybackState() {
        if (!::musicPlayer.isInitialized || !::mediaSession.isInitialized) return
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
                isCurrentSongFavorite = if (currentSongId != -1L) favoriteRepository.isFavorite(currentSongId) else false
                updateNotification()
                updatePlaybackState()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        stopSeekbarUpdates()
        musicPlayer.release()
        notificationManager.release()
        mediaSession.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceScope.cancel()
    }
    @Suppress("DEPRECATION")
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        saveLastSong()
        stopSelf()
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
            mediaItems.add(MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
        }
        result.sendResult(mediaItems)
    }
    private fun downloadToCache(url: String, cacheKey: String) {
        try {
            val uri = url.toUri()
            if (uri.scheme == "content" || uri.scheme == "file") return
            val cache = MusicCache.get(applicationContext)
            val preloadSize = 500L * 1024L
            if (cache.isCached(cacheKey, 0, preloadSize)) return
            val httpDataSourceFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
            val upstreamDataSourceFactory = DefaultDataSource.Factory(applicationContext, httpDataSourceFactory)
            val cacheDataSource = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(upstreamDataSourceFactory)
                .createDataSource()
            val dataSpec = DataSpec.Builder()
                .setUri(uri)
                .setKey(cacheKey)
                .setLength(preloadSize)
                .build()
            val cacheWriter = CacheWriter(cacheDataSource, dataSpec, null, null)
            cacheWriter.cache()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun prefetchQueueWindow() {
        serviceScope.launch(Dispatchers.IO) {
            val queue = QueueRepositoryImpl.queue.value
            val currentSong = QueueRepositoryImpl.currentSong.value ?: return@launch
            val currentIndex = queue.indexOfFirst { it.id == currentSong.id }
            if (currentIndex == -1) return@launch
            val windowSize = 5
            val startIndex = currentIndex + 1
            val endIndex = (startIndex + windowSize).coerceAtMost(queue.size)
            for (i in startIndex until endIndex) {
                if (!isActive) break
                val songToPreload = queue[i]
                if (songToPreload.isLocal) continue
                try {
                    val playableSong = QueueRepositoryImpl.getPlayableSong(songToPreload)
                    if (playableSong != null && playableSong.url.isNotEmpty()) {
                        downloadToCache(playableSong.url, playableSong.id.toString())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}