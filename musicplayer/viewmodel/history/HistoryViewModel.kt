package com.example.musicplayer.viewmodel.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.local.history.HistoryEntry
import com.example.musicplayer.data.repository.HistoryRepositoryImpl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val historyRepository = HistoryRepositoryImpl(application)
    val history: StateFlow<List<HistoryEntry>> = historyRepository.getHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            historyRepository.deleteEntry(entryId)
        }
    }
    fun clearAll() {
        viewModelScope.launch {
            historyRepository.clear()
        }
    }
}