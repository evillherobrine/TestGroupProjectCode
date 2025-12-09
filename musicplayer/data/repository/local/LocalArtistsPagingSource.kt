package com.example.musicplayer.data.repository.local

import android.content.Context
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.musicplayer.domain.model.LocalArtist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalArtistsPagingSource(private val context: Context) : PagingSource<Int, LocalArtist>() {
    override fun getRefreshKey(state: PagingState<Int, LocalArtist>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LocalArtist> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize
        return withContext(Dispatchers.IO) {
            try {
                val artists = mutableListOf<LocalArtist>()
                val uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Artists._ID,
                    MediaStore.Audio.Artists.ARTIST,
                    MediaStore.Audio.Artists.NUMBER_OF_TRACKS
                )
                val sortOrder = "${MediaStore.Audio.Artists.ARTIST} ASC"
                context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                    if (cursor.moveToPosition(offset)) {
                        do {
                            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID))
                            val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST))
                            val count = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS))
                            artists.add(LocalArtist(id, name, count))
                        } while (cursor.moveToNext() && artists.size < pageSize)
                    }
                }
                val nextKey = if (artists.size < pageSize) null else page + 1
                LoadResult.Page(data = artists, prevKey = null, nextKey = nextKey)
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }
}