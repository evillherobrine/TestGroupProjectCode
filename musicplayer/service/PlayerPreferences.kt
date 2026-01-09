package com.example.musicplayer.service

import android.content.Context
import androidx.core.content.edit
import com.example.musicplayer.domain.model.Song

class PlayerPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    companion object {
        const val PREFS_NAME = "MusicPlayerPrefs"
        const val KEY_LAST_SONG_TITLE = "LAST_SONG_TITLE"
        const val KEY_LAST_SONG_ARTIST = "LAST_SONG_ARTIST"
        const val KEY_LAST_SONG_URL = "LAST_SONG_URL"
        const val KEY_LAST_SONG_COVER = "LAST_SONG_COVER"
        const val KEY_LAST_SONG_COVER_XL = "LAST_SONG_COVER_XL"
        const val KEY_LAST_SONG_ID = "LAST_SONG_ID"
    }
    fun saveLastSong(song: Song) {
        if (song.url.isEmpty()) return
        prefs.edit {
            putString(KEY_LAST_SONG_URL, song.url)
            putString(KEY_LAST_SONG_TITLE, song.title)
            putString(KEY_LAST_SONG_ARTIST, song.artist)
            putString(KEY_LAST_SONG_COVER, song.cover)
            putString(KEY_LAST_SONG_COVER_XL, song.coverXL)
            putLong(KEY_LAST_SONG_ID, song.id)
        }
    }
    fun loadLastSong(): Song? {
        val url = prefs.getString(KEY_LAST_SONG_URL, null) ?: return null
        return Song(
            id = prefs.getLong(KEY_LAST_SONG_ID, -1L),
            title = prefs.getString(KEY_LAST_SONG_TITLE, "") ?: "",
            artist = prefs.getString(KEY_LAST_SONG_ARTIST, "") ?: "",
            url = url,
            cover = prefs.getString(KEY_LAST_SONG_COVER, "") ?: "",
            coverXL = prefs.getString(KEY_LAST_SONG_COVER_XL, "") ?: "",
            isLocal = false
        )
    }
}