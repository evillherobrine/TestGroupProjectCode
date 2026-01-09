package com.example.musicplayer.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.musicplayer.data.api.ApiHelper
import com.example.musicplayer.data.local.queue.QueueDao
import com.example.musicplayer.data.local.queue.QueueEntry
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.QueueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class QueueRepositoryImpl(
    private val queueDao: QueueDao,
    private val prefs: SharedPreferences
) : QueueRepository {
    companion object {
        private const val KEY_CURRENT_SONG_ID = "current_song_id"
        private const val URL_EXPIRATION_MS = 3_600_000L
    }
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    override val queue = _queue.asStateFlow()
    private val _currentSong = MutableStateFlow<Song?>(null)
    override val currentSong = _currentSong.asStateFlow()
    private var currentIndex = -1
    suspend fun loadQueueFromDatabase() {
        val savedQueue = queueDao.getQueueFlow().first().map { it.toSong() }
        _queue.value = savedQueue
        val lastSongId = prefs.getLong(KEY_CURRENT_SONG_ID, -1L)
        if (lastSongId != -1L) {
            val lastSong = savedQueue.find { it.id == lastSongId }
            _currentSong.value = lastSong
            currentIndex = savedQueue.indexOf(lastSong)
        }
    }
    suspend fun saveQueueToDatabase() {
        val currentQueue = _queue.value
        val entries = currentQueue.mapIndexed { index, song ->
            QueueEntry(
                songId = song.id,
                queueOrder = index,
                title = song.title,
                artist = song.artist,
                cover = song.cover,
                duration = song.duration,
                url = song.url,
                lastFetchTime = song.lastFetchTime,
                isLocal = song.isLocal
            )
        }
        queueDao.clearQueue()
        queueDao.upsertAll(entries)
    }
    override fun add(song: Song): Boolean {
        val currentQueue = _queue.value.toMutableList()
        if (currentQueue.any { it.id == song.id && it.isLocal == song.isLocal }) return false
        currentQueue.add(song)
        _queue.value = currentQueue
        return true
    }
    override fun remove(song: Song) {
        val currentQueue = _queue.value.toMutableList()
        val songInQueue = currentQueue.find { it.id == song.id } ?: return
        val removedIndex = currentQueue.indexOf(songInQueue)
        currentQueue.remove(songInQueue)
        _queue.value = currentQueue
        if (song.id == _currentSong.value?.id) {
            val nextSong = if (currentQueue.isNotEmpty()) {
                currentQueue[removedIndex.coerceAtMost(currentQueue.size - 1)]
            } else {
                null
            }
            setCurrentSong(nextSong)
        } else {
            if (removedIndex < currentIndex) {
                currentIndex--
            }
        }
    }
    override fun playNext(): Song? {
        val currentQueue = _queue.value
        return if (currentIndex != -1 && currentIndex + 1 < currentQueue.size) {
            currentIndex++
            _currentSong.value = currentQueue[currentIndex]
            _currentSong.value
        } else null
    }
    override fun playPrevious(): Song? {
        val currentQueue = _queue.value
        return if (currentIndex > 0) {
            currentIndex--
            _currentSong.value = currentQueue[currentIndex]
            _currentSong.value
        } else null
    }
    override fun setCurrentSong(song: Song?) {
        _currentSong.value = song
        currentIndex = if (song != null) _queue.value.indexOfFirst { it.id == song.id } else -1
        prefs.edit {
            if (song != null) {
                putLong(KEY_CURRENT_SONG_ID, song.id)
            } else {
                remove(KEY_CURRENT_SONG_ID)
            }
        }
    }
    override suspend fun getPlayableSong(song: Song): Song? {
        if (song.isLocal) return song
        val currentTime = System.currentTimeMillis()
        val isUrlInvalid = song.url.isBlank()
        val isUrlExpired = (currentTime - song.lastFetchTime) > URL_EXPIRATION_MS
        if (isUrlInvalid || isUrlExpired) {
            val songWithNewUrl = ApiHelper.getPlayableSong(song)
            if (songWithNewUrl != null) {
                val currentQueue = _queue.value
                val updatedQueue = currentQueue.map {
                    if (it.id == songWithNewUrl.id) songWithNewUrl else it
                }
                _queue.value = updatedQueue
                return songWithNewUrl
            } else {
                return null
            }
        }
        return song
    }
    override fun clearQueue() {
        _queue.value = emptyList()
        currentIndex = -1
        setCurrentSong(null)
    }
    override fun getNextSong(): Song? {
        val currentQueue = _queue.value
        if (currentIndex == -1 || currentIndex + 1 >= currentQueue.size) {
            return null
        }
        return currentQueue[currentIndex + 1]
    }
    override fun moveSongInQueue(fromPosition: Int, toPosition: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (fromPosition == toPosition || fromPosition >= currentQueue.size || toPosition >= currentQueue.size || fromPosition < 0 || toPosition < 0) {
            return
        }
        val item = currentQueue.removeAt(fromPosition)
        currentQueue.add(toPosition, item)
        _queue.value = currentQueue

        _currentSong.value?.let {
            currentIndex = currentQueue.indexOfFirst { s -> s.id == it.id }
        }
    }
    override fun shuffle() {
        val songToKeep = _currentSong.value ?: return
        val currentQueue = _queue.value
        val otherSongs = currentQueue.filter { it.id != songToKeep.id }.shuffled()
        val newQueue = mutableListOf(songToKeep).apply { addAll(otherSongs) }
        _queue.value = newQueue
        currentIndex = 0
    }
}