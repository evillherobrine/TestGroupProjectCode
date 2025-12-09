package com.example.musicplayer.viewmodel.local

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.musicplayer.data.repository.local.LocalAudioRepository
import com.example.musicplayer.domain.model.LocalAlbum
import com.example.musicplayer.domain.model.LocalArtist
import com.example.musicplayer.domain.model.LocalFolder
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.model.SortDirection
import com.example.musicplayer.domain.model.SortOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
@OptIn(ExperimentalCoroutinesApi::class)
class LocalMusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalAudioRepository(application)
    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()
    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()
    private val _sortDirection = MutableStateFlow(SortDirection.ASC)
    val sortDirection: StateFlow<SortDirection> = _sortDirection.asStateFlow()
    private val _albumSortOption = MutableStateFlow(SortOption.TITLE)
    val albumSortOption: StateFlow<SortOption> = _albumSortOption.asStateFlow()
    private val _albumSortDirection = MutableStateFlow(SortDirection.ASC)
    val albumSortDirection: StateFlow<SortDirection> = _albumSortDirection.asStateFlow()
    private val _artistSortOption = MutableStateFlow(SortOption.TITLE)
    val artistSortOption: StateFlow<SortOption> = _artistSortOption.asStateFlow()
    private val _artistSortDirection = MutableStateFlow(SortDirection.ASC)
    val artistSortDirection: StateFlow<SortDirection> = _artistSortDirection.asStateFlow()
    val localMusicFlow: Flow<PagingData<Song>> = combine(
        _hasPermission,
        _sortOption,
        _sortDirection
    ) { permission, option, direction ->
        Triple(permission, option, direction)
    }.flatMapLatest { (granted, option, direction) ->
        if (granted) {
            repository.getLocalAudioPaging(option, direction)
        } else {
            emptyFlow()
        }
    }.cachedIn(viewModelScope)
    val localAlbumsFlow: Flow<PagingData<LocalAlbum>> = combine(
        _hasPermission,
        _albumSortOption,
        _albumSortDirection
    ) { permission, option, direction ->
        Triple(permission, option, direction)
    }.flatMapLatest { (granted, option, direction) ->
        if (granted) repository.getLocalAlbumsPaging(option, direction) else emptyFlow()
    }.cachedIn(viewModelScope)
    val localArtistsFlow: Flow<PagingData<LocalArtist>> = combine(
        _hasPermission,
        _artistSortOption,
        _artistSortDirection
    ) { permission, option, direction ->
        Triple(permission, option, direction)
    }.flatMapLatest { (granted, option, direction) ->
        if (granted) repository.getLocalArtistsPaging(option, direction) else emptyFlow()
    }.cachedIn(viewModelScope)
    fun updatePermissionStatus(granted: Boolean) {
        _hasPermission.value = granted
    }
    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }
    fun toggleSortDirection() {
        _sortDirection.value = if (_sortDirection.value == SortDirection.ASC)
            SortDirection.DESC
        else
            SortDirection.ASC
    }
    fun updateAlbumSortOption(option: SortOption) { _albumSortOption.value = option }
    fun toggleAlbumSortDirection() {
        _albumSortDirection.value = if (_albumSortDirection.value == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
    }
    fun updateArtistSortOption(option: SortOption) { _artistSortOption.value = option }
    fun toggleArtistSortDirection() {
        _artistSortDirection.value = if (_artistSortDirection.value == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
    }
    val localFoldersFlow: Flow<PagingData<LocalFolder>> = _hasPermission
        .flatMapLatest { granted ->
            if (granted) repository.getLocalFoldersPaging() else emptyFlow()
        }
        .cachedIn(viewModelScope)
}