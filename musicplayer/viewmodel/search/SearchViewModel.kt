package com.example.musicplayer.viewmodel.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.musicplayer.data.repository.history.SearchHistoryRepository
import com.example.musicplayer.data.repository.SearchRepositoryImpl
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.usecase.SearchUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val searchUseCase = SearchUseCase(SearchRepositoryImpl())
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _liveQuery = MutableStateFlow("")
    val liveQuery: StateFlow<String> = _liveQuery.asStateFlow()
    private val historyRepo = SearchHistoryRepository(application.applicationContext)
    val history: Flow<List<String>> = historyRepo.history
    private val apiKeywords = listOf("Deco*27", "Hatsune Miku", "Septette for the dead princess", "U.N Owen was her", "Summer Pockets", "Wowaka", "Unknown Mother Goose")
    val suggestions: StateFlow<List<SearchSuggestion>> =
        _liveQuery
            .combine(history) { liveQuery, historyList ->
                if (liveQuery.isBlank()) {
                    historyList.map { SearchSuggestion(it, SuggestionType.HISTORY) }
                } else {
                    val filteredHistory = historyList
                        .filter { it.contains(liveQuery, ignoreCase = true) }
                        .map { SearchSuggestion(it, SuggestionType.HISTORY) }
                    val historySet = historyList.map { it.lowercase() }.toSet()
                    val filteredApi = apiKeywords
                        .filter { it.contains(liveQuery, ignoreCase = true) }
                        .filter { it.lowercase() !in historySet }
                        .map { SearchSuggestion(it, SuggestionType.API) }
                    filteredHistory + filteredApi
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun onQueryChange(newQuery: String) {
        _liveQuery.value = newQuery
    }

    fun setQuery(query: String) {
        val trimmed = query.trim()
        _query.value = trimmed
        _liveQuery.value = trimmed
    }

    fun saveSuccessfulQuery(query: String, hasEnoughResults: Boolean) {
        if (query.isBlank()) return
        viewModelScope.launch {
            historyRepo.addSearchHistory(query, hasEnoughResults)
        }
    }

    fun deleteHistoryItem(query: String) {
        viewModelScope.launch {
            historyRepo.deleteKeywordHistory(query)
        }
    }

    val tracksFlow: Flow<PagingData<Song>> = _query.flatMapLatest { q ->
        searchUseCase.searchTracks(q)
    }.cachedIn(viewModelScope)

    val playlistsFlow: Flow<PagingData<Playlist>> = _query.flatMapLatest { q ->
        searchUseCase.searchPlaylists(q)
    }.cachedIn(viewModelScope)

    data class SearchSuggestion(
        val query: String,
        val type: SuggestionType
    )

    enum class SuggestionType {
        HISTORY,
        API
    }
}