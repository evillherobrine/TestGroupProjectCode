package com.example.musicplayer.data.repository.local

import android.content.Context
import android.provider.MediaStore
import com.example.musicplayer.domain.model.Song
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.musicplayer.domain.model.LocalAlbum
import com.example.musicplayer.domain.model.LocalArtist
import kotlinx.coroutines.flow.Flow

class LocalAudioRepository(private val context: Context) {
    fun getLocalAudioPaging(): Flow<PagingData<Song>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = { LocalMusicPagingSource(context) }
        ).flow
    }
    fun getLocalAlbumsPaging(): Flow<PagingData<LocalAlbum>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { LocalAlbumsPagingSource(context) }
        ).flow
    }
    fun getLocalArtistsPaging(): Flow<PagingData<LocalArtist>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { LocalArtistsPagingSource(context) }
        ).flow
    }
    fun getSongsByAlbum(albumId: Long): Flow<PagingData<Song>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                LocalDetailPagingSource(
                    context,
                    "${MediaStore.Audio.Media.ALBUM_ID} = ?",
                    arrayOf(albumId.toString())
                )
            }
        ).flow
    }
    fun getSongsByArtist(artistId: Long): Flow<PagingData<Song>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                LocalDetailPagingSource(
                    context,
                    "${MediaStore.Audio.Media.ARTIST_ID} = ?",
                    arrayOf(artistId.toString())
                )
            }
        ).flow
    }
}