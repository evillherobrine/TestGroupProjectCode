package com.example.musicplayer.data.repository.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.musicplayer.domain.model.LocalAlbum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

class LocalAlbumsPagingSource(private val context: Context) : PagingSource<Int, LocalAlbum>() {
    override fun getRefreshKey(state: PagingState<Int, LocalAlbum>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LocalAlbum> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize
        return withContext(Dispatchers.IO) {
            try {
                val albums = mutableListOf<LocalAlbum>()
                val uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Albums._ID,
                    MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums.ARTIST
                )
                val sortOrder = "${MediaStore.Audio.Albums.ALBUM} ASC"
                context.contentResolver.query(uri, projection, null, null, sortOrder)
                    ?.use { cursor ->
                        if (cursor.moveToPosition(offset)) {
                            do {
                                val id =
                                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID))
                                val name =
                                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM))
                                val artist =
                                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST))
                                val artUri = ContentUris.withAppendedId(
                                    "content://media/external/audio/albumart".toUri(),
                                    id
                                ).toString()
                                albums.add(LocalAlbum(id, name, artist, artUri))
                            } while (cursor.moveToNext() && albums.size < pageSize)
                        }
                    }

                val nextKey = if (albums.size < pageSize) null else page + 1
                LoadResult.Page(data = albums, prevKey = null, nextKey = nextKey)
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }
}