package com.example.musicplayer.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.ui.screen.search.PlaylistGridItem

@Composable
fun SuggestedPlaylistsRow(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    onPlaylistLongClick: (Playlist) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistGridItem(
                playlist = playlist,
                onPlaylistClick = { onPlaylistClick(playlist) },
                onPlaylistLongClick = { onPlaylistLongClick(playlist) }
            )
        }
    }
}