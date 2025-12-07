package com.example.musicplayer.data.api

import com.google.gson.annotations.SerializedName

data class SoundCloudPlaylist(
    val id: Long,
    val title: String,
    @SerializedName("track_count")
    val trackCount: Int,
    @SerializedName("artwork_url")
    val artworkUrl: String?,
    val user: User
)