package com.example.musicplayer.ui.screen.local

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.screen.search.TrackItem
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.local.LocalDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable@UnstableApi
fun LocalDetailScreen(
    title: String,
    type: LocalDetailViewModel.DetailType,
    id: Long,
    path: String? = null,
    playerViewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    onShowSongOptions: (Song) -> Unit,
    bottomBarPadding: Dp
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: LocalDetailViewModel = viewModel(
        factory = LocalDetailViewModel.Factory(
            application = application,
            type = type,
            id = id,
            path = path
        )
    )
    val pagingItems = viewModel.songs.collectAsLazyPagingItems()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = bottomBarPadding)
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
                            onClick = { playerViewModel.playSongList(song, pagingItems.itemSnapshotList.items) },
                            onLongClick = { onShowSongOptions(song) }
                        )
                    }
                }
            }
            if (pagingItems.itemCount > 0) {
                FloatingActionButton(
                    onClick = {
                        pagingItems.itemSnapshotList.items.firstOrNull()?.let {
                            playerViewModel.playSongList(it, pagingItems.itemSnapshotList.items)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = bottomBarPadding + 16.dp, end = 16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play All")
                }
            }
        }
    }
}