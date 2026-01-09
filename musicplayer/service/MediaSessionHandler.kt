package com.example.musicplayer.service

import android.app.Service
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.example.musicplayer.R
import com.example.musicplayer.viewmodel.playback.MusicPlayer

class MediaSessionHandler(
    service: Service,
    private val musicPlayer: MusicPlayer,
    private val onAction: (String, Bundle?) -> Unit
) {
    val mediaSession: MediaSessionCompat = MediaSessionCompat(service, "MusicService")
    init {
        val callback = object : MediaSessionCompat.Callback() {
            override fun onPlay() = onAction(MusicService.TOGGLE_PLAY, null)
            override fun onPause() = onAction(MusicService.TOGGLE_PLAY, null)
            override fun onSkipToNext() = onAction(MusicService.NEXT, null)
            override fun onSkipToPrevious() = onAction(MusicService.PREVIOUS, null)
            override fun onSeekTo(pos: Long) {
                val bundle = Bundle().apply { putLong("SEEK_TO", pos) }
                onAction(MusicService.SEEK_TO, bundle)
            }
            override fun onCustomAction(action: String, extras: Bundle?) {
                onAction(action, extras)
            }
        }
        mediaSession.setCallback(callback)
        mediaSession.isActive = true
    }
    fun getSessionToken(): MediaSessionCompat.Token = mediaSession.sessionToken
    fun updatePlaybackState(isFavorite: Boolean) {
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
        val favoriteIcon = if (isFavorite) R.drawable.favorite_checked else R.drawable.favorite_24px
        val favoriteTitle = if (isFavorite) "Unlike" else "Like"
        playbackStateBuilder.addCustomAction(
            PlaybackStateCompat.CustomAction.Builder(
                MusicService.CUSTOM_ACTION_TOGGLE_FAVORITE,
                favoriteTitle,
                favoriteIcon
            ).build()
        )
        mediaSession.setPlaybackState(playbackStateBuilder.build())
    }
    fun release() {
        mediaSession.isActive = false
        mediaSession.release()
    }
}