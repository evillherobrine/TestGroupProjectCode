package com.example.musicplayer.data.repository

import android.content.Context
import com.example.musicplayer.domain.model.Song
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
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
}