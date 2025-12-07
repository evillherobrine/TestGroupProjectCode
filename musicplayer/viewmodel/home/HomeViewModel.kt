package com.example.musicplayer.viewmodel.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.api.BatchSearchRequest
import com.example.musicplayer.data.api.RetrofitClient
import com.example.musicplayer.domain.model.toSong
import com.example.musicplayer.data.repository.SearchHistoryRepository
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.model.toPlaylist
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val historyRepository = SearchHistoryRepository(application)
    private val api = RetrofitClient.api
    private val defaultKeywords = listOf("#Song", "#Viral", "#Popular", "#Trending", "#Pop")
    private val _suggestedSongs = MutableLiveData<List<Song>>()
    val suggestedSongs: LiveData<List<Song>> = _suggestedSongs
    private val _suggestedPlaylists = MutableLiveData<List<Playlist>>()
    val suggestedPlaylists: LiveData<List<Playlist>> = _suggestedPlaylists
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    init {
        loadSuggestedContent()
    }
    private fun loadSuggestedContent() {
        if (_suggestedSongs.value.isNullOrEmpty() && _suggestedPlaylists.value.isNullOrEmpty()) {
            _isLoading.value = true
            viewModelScope.launch {
                try {
                    supervisorScope {
                        val finalKeywords = getFinalKeywords()
                        val tracksRequest = BatchSearchRequest(queries = finalKeywords, limit = 6)
                        val playlistsRequest = BatchSearchRequest(queries = finalKeywords, limit = 3)
                        val tracksJob = async { api.searchBatch(tracksRequest) }
                        val playlistsJob = async { api.searchBatchPlaylists(playlistsRequest) }
                        val trackItems = tracksJob.await()
                        val playlistItems = playlistsJob.await()
                        var songs = trackItems.map { it.toSong() }
                        if (songs.isEmpty()) {
                            val fallbackItems = api.searchTrack(query = "trending", page = 1, limit = 20)
                            songs = fallbackItems.map { it.toSong() }
                        }
                        var playlists = playlistItems.map { it.toPlaylist() }
                        if (playlists.isEmpty()) {
                            val fallbackPlaylists = api.searchPlaylists(query = "top playlists", page = 1, limit = 8)
                            playlists = fallbackPlaylists.map { it.toPlaylist() }
                        }
                        _suggestedSongs.value = songs.shuffled()
                        _suggestedPlaylists.value = playlists
                    }
                } catch (_: Exception) {
                    _suggestedSongs.value = emptyList()
                    _suggestedPlaylists.value = emptyList()
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    private suspend fun getFinalKeywords(): List<String> {
        val historyKeywords = historyRepository.getRandomHistoryKeywords(5)
        return if (historyKeywords.isEmpty()) {
            defaultKeywords
        } else if (historyKeywords.size < 5) {
            val needed = 5 - historyKeywords.size
            val remainingDefaults = defaultKeywords.takeLast(needed)
            historyKeywords + remainingDefaults
        } else {
            historyKeywords
        }
    }
}