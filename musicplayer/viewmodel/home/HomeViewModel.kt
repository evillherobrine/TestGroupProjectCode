package com.example.musicplayer.viewmodel.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.api.RetrofitClient
import com.example.musicplayer.data.repository.history.HistoryRepositoryImpl
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.model.toSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val historyRepository = HistoryRepositoryImpl(application)
    private val api = RetrofitClient.api
    val recentSongs: StateFlow<List<Song>> = historyRepository.getRecentForHome()
        .map { historyList ->
            historyList.map { it.toSong() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    private val _trendingSongs = MutableStateFlow<List<Song>>(emptyList())
    val trendingSongs: StateFlow<List<Song>> = _trendingSongs.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadTrendingContent()
    }
    private fun loadTrendingContent() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val trendingResponse = api.searchTrack(
                    query = "trending music",
                    page = 1,
                    limit = 50
                )
                val songs = trendingResponse
                    .shuffled()
                    .take(20)
                    .map { it.toSong() }

                _trendingSongs.value = songs
            } catch (e: Exception) {
                e.printStackTrace()
                _trendingSongs.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}