package com.example.musicplayer.domain.repository

import androidx.paging.PagingData
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow
interface SearchRepository {
    fun searchTracks(query: String): Flow<PagingData<Song>>
    fun searchPlaylists(query: String): Flow<PagingData<Playlist>>
}
