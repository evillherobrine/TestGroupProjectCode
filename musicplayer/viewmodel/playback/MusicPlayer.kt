package com.example.musicplayer.viewmodel.playback

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MusicPlayer(context: Context) {
    internal val exoPlayer: ExoPlayer = run {
        val audioAttributes = AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build()
        ExoPlayer.Builder(context).setAudioAttributes(audioAttributes,true).setHandleAudioBecomingNoisy(true).build()
    }
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
    fun play(url: String) {
        val mediaItem = MediaItem.fromUri(url.toUri())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()
    }
    fun prepare(url: String) {
        val mediaItem = MediaItem.fromUri(url.toUri())
        exoPlayer.setMediaItem(mediaItem)
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
    }
}
