package com.example.musicplayer.data.repository.history

import android.content.Context
import com.example.musicplayer.data.local.search.SearchHistoryEntry
import com.example.musicplayer.data.local.search.SearchHistoryDao
import com.example.musicplayer.data.local.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchHistoryRepository(context: Context) {
    private val dao: SearchHistoryDao = AppDatabase.getDatabase(context).searchQueryDao()
    val history: Flow<List<String>> = dao.searchHistoryFlow().map { list ->
        list.map { it.query }
    }
    suspend fun addSearchHistory(query: String, hasEnoughResults: Boolean) {
        if (query.isBlank()) return
        dao.insert(SearchHistoryEntry(
            query = query,
            timestamp = System.currentTimeMillis(),
            hasEnoughResults = hasEnoughResults
        ))
    }
    suspend fun deleteKeywordHistory(query: String) {
        dao.keywordDelete(query)
    }
    suspend fun getRandomHistoryKeywords(limit: Int = 5): List<String> {
        return dao.getRandomEntries(limit).map { it.query }
    }
}