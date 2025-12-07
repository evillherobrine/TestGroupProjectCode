package com.example.musicplayer.viewmodel.playlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.api.RetrofitClient
import com.example.musicplayer.data.api.SoundCloudResponseItem
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.launch

sealed class PlaylistDetailState {
    object Loading : PlaylistDetailState()
    data class Success(val songs: List<Song>) : PlaylistDetailState()
    data class Error(val message: String) : PlaylistDetailState()
}
class OnlinePlaylistViewModel : ViewModel() {
    private val _playlistState = MutableLiveData<PlaylistDetailState>()
    val playlistState: LiveData<PlaylistDetailState> = _playlistState
    fun fetchPlaylistTracks(playlistId: Long) {
        _playlistState.value = PlaylistDetailState.Loading
        viewModelScope.launch {
            try {
                val tracks = RetrofitClient.api.getPlaylistTracks(playlistId)
                val mappedSongs = tracks.map { item ->
                    mapToSong(item)
                }

                _playlistState.value = PlaylistDetailState.Success(mappedSongs)

            } catch (_: Exception) {
                _playlistState.value = PlaylistDetailState.Error("Can't load track list")
            }
        }
    }
    private fun mapToSong(item: SoundCloudResponseItem): Song {
        val largeArtworkUrl = item.artwork_url?.replace("-large.jpg", "-t500x500.jpg") ?: ""
        return Song(
            id = item.id,
            title = item.title,
            url = "",
            artist = item.metadata_artist?.takeIf { it.isNotBlank() } ?: item.user.username.ifEmpty { item.user.full_name.ifEmpty { "Unknown" } },
            cover = item.artwork_url,
            coverXL = largeArtworkUrl,
            duration = item.duration.toLong(),
            lastFetchTime = 0L
        )
    }
}