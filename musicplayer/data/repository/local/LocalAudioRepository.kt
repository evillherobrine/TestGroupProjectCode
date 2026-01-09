package com.example.musicplayer.data.repository.local

import android.content.Context
import android.provider.MediaStore
import com.example.musicplayer.domain.model.Song
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.musicplayer.domain.model.LocalAlbum
import com.example.musicplayer.domain.model.LocalArtist
import com.example.musicplayer.domain.model.LocalFolder
import com.example.musicplayer.domain.model.SortDirection
import com.example.musicplayer.domain.model.SortOption
import kotlinx.coroutines.flow.Flow

class LocalAudioRepository(private val context: Context) {
    fun getLocalAudioPaging(
        sortOption: SortOption = SortOption.TITLE,
        sortDirection: SortDirection = SortDirection.ASC
    ): Flow<PagingData<Song>> {
        val sortOrder = "${sortOption.column} ${sortDirection.sql}"
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = { LocalMusicPagingSource(context, sortOrder) }
        ).flow
    }
    fun getLocalAlbumsPaging(
        sortOption: SortOption = SortOption.TITLE,
        sortDirection: SortDirection = SortDirection.ASC
    ): Flow<PagingData<LocalAlbum>> {
        val sortColumn = when (sortOption) {
            SortOption.TITLE -> MediaStore.Audio.Albums.ALBUM
            SortOption.ARTIST -> MediaStore.Audio.Albums.ARTIST
            else -> MediaStore.Audio.Albums.ALBUM
        }
        val sortOrder = "$sortColumn ${sortDirection.sql}"
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { LocalAlbumsPagingSource(context,sortOrder) }
        ).flow
    }
    fun getLocalArtistsPaging(
        sortOption: SortOption = SortOption.TITLE,
        sortDirection: SortDirection = SortDirection.ASC
    ): Flow<PagingData<LocalArtist>> {
        val sortColumn = when (sortOption) {
            SortOption.TITLE -> MediaStore.Audio.Artists.ARTIST
            else -> MediaStore.Audio.Artists.ARTIST
        }
        val sortOrder = "$sortColumn ${sortDirection.sql}"
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { LocalArtistsPagingSource(context, sortOrder) }
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
    fun getLocalFoldersPaging(): Flow<PagingData<LocalFolder>> {
        return Pager(
            config = PagingConfig(pageSize = 50),
            pagingSourceFactory = { LocalFoldersPagingSource(context) }
        ).flow
    }
    fun getSongsByFolder(folderPath: String): Flow<PagingData<Song>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                LocalDetailPagingSource(
                    context,
                    "${MediaStore.Audio.Media.DATA} LIKE ?",
                    arrayOf("$folderPath/%")
                )
            }
        ).flow
    }
}