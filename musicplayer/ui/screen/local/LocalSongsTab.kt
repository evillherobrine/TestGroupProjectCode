package com.example.musicplayer.ui.screen.local

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.screen.component.SortHeader
import com.example.musicplayer.ui.screen.search.TrackItem
import com.example.musicplayer.viewmodel.local.LocalMusicViewModel
import com.example.musicplayer.viewmodel.playback.PlayerViewModel

@Composable
@UnstableApi
fun LocalSongsTab(
    viewModel: LocalMusicViewModel,
    playerViewModel: PlayerViewModel,
    onShowSongOptions: (Song) -> Unit,
    bottomPadding: Dp,
    scrollToTop: Long
) {
    val pagingItems = viewModel.localMusicFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val sortOption by viewModel.sortOption.collectAsState()
    val sortDirection by viewModel.sortDirection.collectAsState()
    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0) {
            listState.animateScrollToItem(0)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        SortHeader(
            sortOption = sortOption,
            sortDirection = sortDirection,
            itemCount = pagingItems.itemCount,
            itemLabel = "songs",
            onSortOptionSelected = { viewModel.updateSortOption(it) },
            onSortDirectionToggle = { viewModel.toggleSortDirection() }
        )
        if (pagingItems.loadState.refresh is LoadState.Loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (pagingItems.itemCount == 0 && pagingItems.loadState.refresh is LoadState.NotLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No music files found")
            }
        } else {
            Box(Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomPadding)
                ) {
                    items(
                        count = pagingItems.itemCount,
                        key = pagingItems.itemKey { it.id },
                        contentType = pagingItems.itemContentType { "song" }
                    ) { index ->
                        val song = pagingItems[index]
                        if (song != null) {
                            TrackItem(
                                song = song,
                                onClick = {
                                    playerViewModel.playSongList(song, pagingItems.itemSnapshotList.items)
                                },
                                onLongClick = { onShowSongOptions(song) }
                            )
                        }
                    }
                    if (pagingItems.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = {
                        pagingItems.itemSnapshotList.items.firstOrNull()?.let { firstSong ->
                            playerViewModel.playSongList(firstSong, pagingItems.itemSnapshotList.items)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = bottomPadding + 16.dp, end = 16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play All")
                }
            }
        }
    }
}