package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

class FavoriteUseCase(favoriteRepository: FavoriteRepository) {
    val favoriteSongs: Flow<List<Song>> = favoriteRepository.favoriteSongs
}
