package com.example.musicplayer.data.repository.local

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

class LocalMusicPagingSource(
    private val context: Context
) : PagingSource<Int, Song>() {

    override fun getRefreshKey(state: PagingState<Int, Song>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
        val page = params.key ?: 1
        val pageSize = params.loadSize
        val offset = (page - 1) * pageSize

        return withContext(Dispatchers.IO) {
            try {
                val songs = mutableListOf<Song>()
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM_ID
                )
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"
                val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
                context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    null,
                    sortOrder
                )?.use { cursor ->
                    if (cursor.moveToPosition(offset)) {
                        do {
                            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                            val id = cursor.getLong(idColumn)
                            val title = cursor.getString(titleColumn)
                            val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                            val duration = cursor.getLong(durationColumn)
                            val albumId = cursor.getLong(albumIdColumn)
                            val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                            val albumArtUri = ContentUris.withAppendedId(
                                "content://media/external/audio/albumart".toUri(),
                                albumId
                            ).toString()
                            songs.add(
                                Song(
                                    id = id,
                                    title = title,
                                    url = contentUri.toString(),
                                    artist = artist,
                                    cover = albumArtUri,
                                    coverXL = albumArtUri,
                                    duration = duration,
                                    isLocal = true
                                )
                            )
                        } while (cursor.moveToNext() && songs.size < pageSize)
                    }
                }
                val nextKey = if (songs.size < pageSize) null else page + 1
                val prevKey = if (page == 1) null else page - 1
                LoadResult.Page(
                    data = songs,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }
}