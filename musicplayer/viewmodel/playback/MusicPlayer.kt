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
import com.example.musicplayer.cache.MusicCache

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
        .build()
    val isPlaying: Boolean
        get() = exoPlayer.isPlaying
    val isRepeating: Boolean
        get() = exoPlayer.repeatMode == ExoPlayer.REPEAT_MODE_ONE
    val currentPosition: Long
        get() = exoPlayer.currentPosition
    val duration: Long
        get() = exoPlayer.duration
    val bufferedPosition: Long
        get() = exoPlayer.bufferedPosition
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
    }
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
    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }
    fun toggleRepeatMode() {
        exoPlayer.repeatMode =
            if (exoPlayer.repeatMode == ExoPlayer.REPEAT_MODE_ONE)
                ExoPlayer.REPEAT_MODE_OFF
            else
                ExoPlayer.REPEAT_MODE_ONE
    }
    fun turnOffRepeatOne() {
        if (exoPlayer.repeatMode == ExoPlayer.REPEAT_MODE_ONE) {
            exoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_OFF
        }
    }
    fun addListener(listener: Player.Listener) {
        exoPlayer.addListener(listener)
    }
    fun release() {
        exoPlayer.release()
    }
    fun stop(){
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }
    fun clearMediaItems() {
        exoPlayer.clearMediaItems()
    }
}