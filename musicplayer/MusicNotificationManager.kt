package com.example.musicplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import coil.imageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import com.example.musicplayer.viewmodel.playback.MusicPlayer

@UnstableApi
class MusicNotificationManager(private val service: MusicService, private val musicPlayer: MusicPlayer) {
    private val channelId = "music_channel_id"
    internal val notificationId = 1
    private var currentCoverBitmap: Bitmap? = null
    private var disposable: Disposable? = null
    private val notificationManager = service.getSystemService(NotificationManager::class.java)
    private val playIntent by lazy { getActionIntent(MusicService.TOGGLE_PLAY) }
    private val nextIntent by lazy { getActionIntent(MusicService.NEXT) }
    private val prevIntent by lazy { getActionIntent(MusicService.PREVIOUS) }
    private val favoriteIntent by lazy { getActionIntent(MusicService.TOGGLE_FAVORITE_NOTIFICATION) }
    private val contentIntent by lazy {
        val launchIntent = service.packageManager.getLaunchIntentForPackage(service.packageName)
        PendingIntent.getActivity(
            service,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    fun createNotificationChannel() {
        if (notificationManager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                "Music Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for music playback"
                setShowBadge(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun getActionIntent(action: String): PendingIntent {
        val intent = Intent(service, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            service,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    private fun createNotificationBuilder(mediaSession: MediaSessionCompat, isFavorite: Boolean): NotificationCompat.Builder {
        val isPlaying = musicPlayer.isPlaying
        val title = service.currentTitle.ifEmpty { "Unknown Title" }
        val artist = service.currentArtist.ifEmpty { "Unknown Artist" }
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
        val playPauseIcon = if (isPlaying) R.drawable.pause_24px else R.drawable.play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"
        val favoriteIcon = if (isFavorite) R.drawable.favorite_checked else R.drawable.favorite_24px
        val builder = NotificationCompat.Builder(service, channelId)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.music_note_24px)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(mediaStyle)
            .setColorized(true)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.skip_previous_24px, "Previous", prevIntent)
            .addAction(playPauseIcon, playPauseTitle, playIntent)
            .addAction(R.drawable.skip_next_24px, "Next", nextIntent)
            .addAction(favoriteIcon, "Favorite", favoriteIntent)
        currentCoverBitmap?.let { builder.setLargeIcon(it) }

        return builder
    }
    fun buildNotification(mediaSession: MediaSessionCompat): Notification {
        return createNotificationBuilder(mediaSession, service.isCurrentSongFavorite).build()
    }
    fun updateNotification(mediaSession: MediaSessionCompat, isFavorite: Boolean) {
        val builder = createNotificationBuilder(mediaSession, isFavorite)
        notificationManager.notify(notificationId, builder.build())
    }
    fun loadCoverArt(coverUrl: String) {
        disposable?.dispose()
        currentCoverBitmap = null
        if (coverUrl.isNotBlank()) {
            val request = ImageRequest.Builder(service)
                .data(coverUrl)
                .allowHardware(false)
                .size(256, 256)
                .target(
                    onSuccess = { result ->
                        currentCoverBitmap = (result as? BitmapDrawable)?.bitmap
                        updateMediaMetadata()
                        service.triggerNotificationUpdate()
                    },
                    onError = {
                        currentCoverBitmap = null
                        updateMediaMetadata()
                        service.triggerNotificationUpdate()
                    }
                )
                .build()
            disposable = service.imageLoader.enqueue(request)
        } else {
            updateMediaMetadata()
            service.triggerNotificationUpdate()
        }
    }
    fun updateMediaMetadata() {
        val duration = if (musicPlayer.duration == C.TIME_UNSET) 0L else musicPlayer.duration
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, service.currentTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, service.currentArtist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
        currentCoverBitmap?.let {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
        }

        service.mediaSession.setMetadata(metadataBuilder.build())
    }
    fun release() {
        disposable?.dispose()
        disposable = null
        currentCoverBitmap = null
    }
}