package com.example.musicplayer.domain.model

import java.io.Serializable

data class Song(
    val id: Long,
    val title: String,
    var url: String,
    val artist: String,
    val cover: String? = "",
    val coverXL: String? = "",
    val duration: Long = 0L,
    var lastFetchTime: Long = 0,
    val isLocal: Boolean = false
): Serializable