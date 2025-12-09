package com.example.musicplayer.domain.model

import android.provider.MediaStore

enum class SortOption(val label: String, val column: String) {
    TITLE("Name", MediaStore.Audio.Media.TITLE),
    DATE_ADDED("Date Added", MediaStore.Audio.Media.DATE_ADDED),
    ARTIST("Artist", MediaStore.Audio.Media.ARTIST),
    DURATION("Duration", MediaStore.Audio.Media.DURATION)
}
enum class SortDirection(val sql: String) {
    ASC("ASC"),
    DESC("DESC")
}