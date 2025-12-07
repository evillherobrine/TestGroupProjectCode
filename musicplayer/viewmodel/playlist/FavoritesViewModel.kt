package com.example.musicplayer.viewmodel.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.repository.FavoriteRepositoryImpl
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.usecase.FavoriteUseCase
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FavoriteRepositoryImpl(application)
    private val favoriteUseCase = FavoriteUseCase(repository)
    val favoriteSongs = favoriteUseCase.favoriteSongs.asLiveData()
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song)
        }
    }
}