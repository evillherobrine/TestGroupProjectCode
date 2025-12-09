package com.example.musicplayer.data.repository.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalDetailPagingSource(
    private val context: Context,
    private val selection: String,
    private val selectionArgs: Array<String>
) : PagingSource<Int, Song>() {

    override fun getRefreshKey(state: PagingState<Int, Song>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Song> {
        val page = params.key ?: 1
        val pageSize = params.loadSize
        val offset = (page - 1) * pageSize

        return withContext(Dispatchers.IO) {
            try {
                val songs = mutableListOf<Song>()
                val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM_ID
                )
                val finalSelection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000 AND $selection"
                val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
                context.contentResolver.query(
                    uri,
                    projection,
                    finalSelection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    if (cursor.moveToPosition(offset)) {
                        do {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                            val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                            val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                            val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                            val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                            val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                            val albumArtUri = ContentUris.withAppendedId(
                                "content://media/external/audio/albumart".toUri(),
                                albumId
                            ).toString()
                            songs.add(Song(id, title, contentUri.toString(), artist, albumArtUri, albumArtUri, duration, isLocal = true))
                        } while (cursor.moveToNext() && songs.size < pageSize)
                    }
                }
                val nextKey = if (songs.size < pageSize) null else page + 1
                LoadResult.Page(data = songs, prevKey = if (page == 1) null else page - 1, nextKey = nextKey)
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }
}