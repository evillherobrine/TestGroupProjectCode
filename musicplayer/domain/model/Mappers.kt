package com.example.musicplayer.domain.model

import com.example.musicplayer.data.api.SoundCloudPlaylist
import com.example.musicplayer.data.api.SoundCloudResponseItem

fun SoundCloudResponseItem.toSong(): Song {
    val highResCover = this.artwork_url?.replace("-large.jpg", "-t500x500.jpg")
    return Song(
        id = this.id,
        title = this.title,
        url = "",
        artist = this.user.username,
        cover = this.artwork_url,
        coverXL = highResCover ?: this.artwork_url,
        duration = this.duration.toLong(),
        lastFetchTime = 0L
    )
}
fun SoundCloudPlaylist.toPlaylist(): Playlist {
    val highResCover = this.artworkUrl?.replace("-large.jpg", "-t300x300.jpg")
    return Playlist(
        id = this.id,
        name = this.title,
        songCount = this.trackCount,
        coverUrl = highResCover ?: this.artworkUrl)
}