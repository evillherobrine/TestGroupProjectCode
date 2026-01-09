package com.example.musicplayer.domain.usecase

import androidx.paging.PagingData
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow

class SearchUseCase(private val searchRepository: SearchRepository) {

    fun searchTracks(query: String): Flow<PagingData<Song>> {
        return searchRepository.searchTracks(query)
    }

    fun searchPlaylists(query: String): Flow<PagingData<Playlist>> {
        return searchRepository.searchPlaylists(query)
    }
}
