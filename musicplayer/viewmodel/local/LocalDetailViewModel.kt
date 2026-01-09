package com.example.musicplayer.viewmodel.local

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.musicplayer.data.repository.local.LocalAudioRepository
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

class LocalDetailViewModel(
    application: Application,
    type: DetailType,
    id: Long,
    private val path: String? = null
) : AndroidViewModel(application) {
    private val repository = LocalAudioRepository(application)
    enum class DetailType { ALBUM, ARTIST, FOLDER }
    val songs: Flow<PagingData<Song>> = when (type) {
        DetailType.ALBUM -> repository.getSongsByAlbum(id)
        DetailType.ARTIST -> repository.getSongsByArtist(id)
        DetailType.FOLDER -> repository.getSongsByFolder(path ?: "")
    }.cachedIn(viewModelScope)
    class Factory(
        private val application: Application,
        private val type: DetailType,
        private val id: Long = 0,
        private val path: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LocalDetailViewModel(application, type, id, path) as T
        }
    }
}