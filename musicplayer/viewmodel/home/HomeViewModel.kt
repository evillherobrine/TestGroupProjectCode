package com.example.musicplayer.viewmodel.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.api.RetrofitClient
import com.example.musicplayer.data.repository.history.HistoryRepositoryImpl
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.model.toSong
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val historyRepository = HistoryRepositoryImpl(application)
    private val _userPersona = MutableStateFlow<Pair<String, String>?>(null)
    val userPersona: StateFlow<Pair<String, String>?> = _userPersona.asStateFlow()
    private val api = RetrofitClient.api
    private val _recentSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentSongs: StateFlow<List<Song>> = _recentSongs.asStateFlow()
    private val _trendingSongs = MutableStateFlow<List<Song>>(emptyList())
    val trendingSongs: StateFlow<List<Song>> = _trendingSongs.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    init {
        loadHomeContent()
        calculateUserPersona()
    }
    private fun calculateUserPersona() {
        viewModelScope.launch {
            _userPersona.value = historyRepository.getUserPersona()
        }
    }
    private fun loadHomeContent() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val recentDeferred = async { historyRepository.getRecentSongs() }
                val trendingDeferred = async {
                    api.searchTrack(
                        query = "trending music",
                        page = 1,
                        limit = 50
                    )
                }
                val historyList = recentDeferred.await()
                _recentSongs.value = historyList.map { it.toSong() }
                val trendingResponse = trendingDeferred.await()
                val songs = trendingResponse
                    .shuffled()
                    .take(20)
                    .map { it.toSong() }
                _trendingSongs.value = songs
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}