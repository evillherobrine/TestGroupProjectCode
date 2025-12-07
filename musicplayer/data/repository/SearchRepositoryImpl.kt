package com.example.musicplayer.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.musicplayer.data.api.BaseSearchPagingSource
import com.example.musicplayer.data.api.RetrofitClient
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
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
                val largeArtworkUrl = item.artwork_url?.replace("-large.jpg", "-t500x500.jpg") ?: ""
                Song(
                    id = item.id,
                    title = item.title,
                    url = "",
                    artist = item.metadata_artist?.takeIf { it.isNotBlank() } ?: item.user.username.ifEmpty { item.user.full_name.ifEmpty { "Unknown" } },
                    cover = item.artwork_url,
                    coverXL = largeArtworkUrl,
                    duration = item.duration.toLong(),
                    lastFetchTime = 0L
                )
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
