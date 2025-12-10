package com.example.musicplayer.domain.model

import java.io.Serializable

data class Song(
    val id: Long,
    val title: String,
    val url: String,
    val artist: String,
    val cover: String? = "",
    val coverXL: String? = "",
    val duration: Long = 0L,
    val lastFetchTime: Long = 0,
    val isLocal: Boolean = false
): Serializable