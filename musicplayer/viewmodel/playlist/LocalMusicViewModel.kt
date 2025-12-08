package com.example.musicplayer.viewmodel.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.musicplayer.data.repository.LocalAudioRepository
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

class LocalMusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalAudioRepository(application)
    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()
    @OptIn(ExperimentalCoroutinesApi::class)
    val localMusicFlow: Flow<PagingData<Song>> = _hasPermission
        .flatMapLatest { granted ->
            if (granted) {
                repository.getLocalAudioPaging()
            } else {
                emptyFlow()
            }
        }
        .cachedIn(viewModelScope)
    fun updatePermissionStatus(granted: Boolean) {
        _hasPermission.value = granted
    }
}