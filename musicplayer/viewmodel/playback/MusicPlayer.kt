package com.example.musicplayer.viewmodel.playback

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.musicplayer.audio.NightModeManager
import com.example.musicplayer.cache.MusicCache
import com.example.musicplayer.domain.model.RepeatMode

@UnstableApi
class MusicPlayer(context: Context) {
    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
    private val upstreamDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(MusicCache.get(context))
        .setUpstreamDataSourceFactory(upstreamDataSourceFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    internal val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory))
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()
    private val nightModeManager: NightModeManager = NightModeManager()
    private var isNightModeInitialized = false
    val isPlaying: Boolean
        get() = exoPlayer.isPlaying
    private var _repeatMode: RepeatMode = RepeatMode.OFF
    val repeatMode: RepeatMode
        get() = _repeatMode
    val currentPosition: Long
        get() = exoPlayer.currentPosition
    val duration: Long
        get() = exoPlayer.duration
    val bufferedPosition: Long
        get() = exoPlayer.bufferedPosition
    val isNightModeEnabled: Boolean
        get() = nightModeManager.isNightModeEnabled()
    val audioSessionId: Int
        get() = exoPlayer.audioSessionId
    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && !isNightModeInitialized) {
                    initializeNightMode()
                }
            }
        })
    }
    private fun initializeNightMode() {
        val sessionId = exoPlayer.audioSessionId
        if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
            nightModeManager.initialize(sessionId)
            isNightModeInitialized = true
        }
    }
    fun play(url: String, cacheKey: String? = null) {
        stop()
        val uri = url.toUri()
        val isLocal = uri.scheme == "content" || uri.scheme == "file" || uri.scheme == "android.resource"
        if (isLocal) {
            val mediaSource = ProgressiveMediaSource.Factory(upstreamDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
            exoPlayer.setMediaSource(mediaSource)
        } else {
            val mediaItemBuilder = MediaItem.Builder().setUri(uri)
            if (!cacheKey.isNullOrEmpty()) {
                mediaItemBuilder.setCustomCacheKey(cacheKey)
            }
            exoPlayer.setMediaItem(mediaItemBuilder.build())
        }
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
        if (isNightModeInitialized) {
            nightModeManager.updateAudioSession(exoPlayer.audioSessionId)
        }}
    fun prepare(url: String) {
        stop()
        val uri = url.toUri()
        val isLocal = uri.scheme == "content" || uri.scheme == "file" || uri.scheme == "android.resource"
        if (isLocal) {
            val mediaSource = ProgressiveMediaSource.Factory(upstreamDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
            exoPlayer.setMediaSource(mediaSource)
        } else {
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.setMediaItem(mediaItem)
        }
        exoPlayer.playWhenReady = false
        exoPlayer.prepare()
    }
    fun isReady(): Boolean {
        return exoPlayer.playbackState != Player.STATE_IDLE
    }
    fun pause() {
        exoPlayer.pause()
    }
    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }
    fun play() {
        exoPlayer.play()
    }
    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }
    fun toggleRepeatMode() {
        _repeatMode = when (_repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        syncExoPlayerRepeatMode()}
    private fun syncExoPlayerRepeatMode() {
        exoPlayer.repeatMode = when (_repeatMode) {
            RepeatMode.ONE -> ExoPlayer.REPEAT_MODE_ONE
            RepeatMode.ALL -> ExoPlayer.REPEAT_MODE_OFF
            RepeatMode.OFF -> ExoPlayer.REPEAT_MODE_OFF
        }}
    fun turnOffRepeatOne() {
        if (_repeatMode == RepeatMode.ONE) {
            _repeatMode = RepeatMode.OFF
            syncExoPlayerRepeatMode()
        }
    }
    fun setNightModeEnabled(enabled: Boolean) {
        if (!isNightModeInitialized) {
            initializeNightMode()
        }
        nightModeManager.setEnabled(enabled)}
    fun toggleNightMode(): Boolean {
        if (!isNightModeInitialized) {
            initializeNightMode()}
        return nightModeManager.toggle()}
    fun addListener(listener: Player.Listener) {
        exoPlayer.addListener(listener)
    }
    fun release() {
        nightModeManager.release()
        exoPlayer.release()
    }
    fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }
    fun clearMediaItems() {
        exoPlayer.clearMediaItems()
    }
}
