package com.example.musicplayer.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.musicplayer.data.api.BaseSearchPagingSource
import com.example.musicplayer.data.api.RetrofitClient
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.model.toSong
import com.example.musicplayer.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchRepositoryImpl : SearchRepository {
    private val api = RetrofitClient.api
    private val trackPagingConfig = PagingConfig(
        pageSize = 20,
        enablePlaceholders = false
    )

    private val playlistPagingConfig = PagingConfig(
        pageSize = 10,
        enablePlaceholders = false
    )

    override fun searchTracks(query: String): Flow<PagingData<Song>> {
        return Pager(
            config = trackPagingConfig,
            pagingSourceFactory = { BaseSearchPagingSource(query, api::searchTrack) }
        ).flow.map { pagingData ->
            pagingData.map { item ->
                item.toSong()
            }
        }
    }

    override fun searchPlaylists(query: String): Flow<PagingData<Playlist>> {
        return Pager(
            config = playlistPagingConfig,
            pagingSourceFactory = { BaseSearchPagingSource(query, api::searchPlaylists) }
        ).flow.map { pagingData ->
            pagingData.map { playlist ->
                Playlist(
                    id = playlist.id,
                    name = playlist.title,
                    songCount = playlist.trackCount,
                    coverUrl = playlist.artworkUrl
                )
            }
        }
    }
}
