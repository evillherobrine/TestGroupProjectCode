package com.example.musicplayer.data.repository.local

import android.content.Context
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.musicplayer.domain.model.LocalFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LocalFoldersPagingSource(private val context: Context) : PagingSource<Int, LocalFolder>() {
    override fun getRefreshKey(state: PagingState<Int, LocalFolder>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LocalFolder> {
        if (params.key != null && params.key!! > 0) {
            return LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
        }
        return withContext(Dispatchers.IO) {
            try {
                val foldersMap = mutableMapOf<String, Int>()
                val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(MediaStore.Audio.Media.DATA)
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                    val dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    while (cursor.moveToNext()) {
                        val filePath = cursor.getString(dataIndex)
                        val file = File(filePath)
                        val parentPath = file.parent
                        if (parentPath != null) {
                            val count = foldersMap.getOrDefault(parentPath, 0)
                            foldersMap[parentPath] = count + 1
                        }
                    }
                }
                val folders = foldersMap.map { (path, count) ->
                    val folderName = path.substringAfterLast("/")
                    LocalFolder(name = folderName, path = path, songCount = count)
                }.sortedBy { it.name }

                LoadResult.Page(data = folders, prevKey = null, nextKey = 1)
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }
}