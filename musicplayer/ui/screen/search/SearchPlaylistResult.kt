package com.example.musicplayer.ui.screen.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.R
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.viewmodel.search.SearchViewModel

@Composable
fun PlaylistResultScreen(
    viewModel: SearchViewModel,
    onPlaylistClick: (Playlist) -> Unit,
    onPlaylistLongClick: (Playlist) -> Unit
) {
    val lazyPlaylistItems = viewModel.playlistsFlow.collectAsLazyPagingItems()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(lazyPlaylistItems.itemCount) { index ->
                lazyPlaylistItems[index]?.let { playlist ->
                    PlaylistGridItem(
                        playlist = playlist,
                        onPlaylistClick = { onPlaylistClick(playlist) },
                        onPlaylistLongClick = { onPlaylistLongClick(playlist) }
                    )
                }
            }
        }

        if (lazyPlaylistItems.loadState.refresh is LoadState.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistGridItem(
    modifier: Modifier = Modifier,
    playlist: Playlist,
    onPlaylistClick: () -> Unit,
    onPlaylistLongClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .width(120.dp)
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = onPlaylistClick,
                onLongClick = onPlaylistLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(playlist.coverUrl)
                .crossfade(true)
                .placeholder(R.drawable.music_note)
                .error(R.drawable.music_note)
                .build(),
            contentDescription = "Playlist Artwork",
            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${playlist.songCount} songs",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}