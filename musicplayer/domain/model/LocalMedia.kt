package com.example.musicplayer.domain.model

data class LocalAlbum(
    val id: Long,
    val name: String,
    val artist: String,
    val coverUri: String
)
data class LocalArtist(
    val id: Long,
    val name: String,
    val trackCount: Int
)
data class LocalFolder(
    val name: String,
    val path: String,
    val songCount: Int
)