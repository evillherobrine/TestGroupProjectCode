package com.example.musicplayer.domain.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object MusicStateRepository {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    private val _isRepeating = MutableStateFlow(false)
    val isRepeating = _isRepeating.asStateFlow()
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()
    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()
    private val _isNightMode = MutableStateFlow(false)
    val isNightMode = _isNightMode.asStateFlow()
    fun updatePlaybackState(isPlaying: Boolean, isRepeating:  Boolean) {
        _isPlaying.value = isPlaying
        _isRepeating.value = isRepeating
    }
    fun updateProgress(position: Long, duration: Long) {
        _currentPosition.value = position
        _duration.value = duration
    }
    fun setNightMode(isActive: Boolean) {
        _isNightMode.value = isActive
    }
}