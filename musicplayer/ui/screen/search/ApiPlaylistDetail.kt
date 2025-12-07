package com.example.musicplayer.ui.screen.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.playlist.PlaylistDetailState
import com.example.musicplayer.viewmodel.playlist.OnlinePlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ApiPlaylistDetail(
    playlistId: Long,
    playlistName: String,
    viewModel: OnlinePlaylistViewModel = viewModel(),
    playerViewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    onShowSongOptions: (Song) -> Unit,
    bottomBarPadding: Dp
) {
    val state by viewModel.playlistState.observeAsState(PlaylistDetailState.Loading)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    LaunchedEffect(playlistId) {
        viewModel.fetchPlaylistTracks(playlistId)
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(playlistName)
                        val currentState = state
                        if (currentState is PlaylistDetailState.Success) {
                            Text(
                                text = "${currentState.songs.size} songs",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            when (val currentState = state) {
                is PlaylistDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is PlaylistDetailState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(),contentPadding = PaddingValues(bottom = bottomBarPadding)) {
                        items(
                            items = currentState.songs,
                            key = { it.id }
                        ) { song ->
                            TrackItem(
                                song = song,
                                onClick = {
                                    playerViewModel.playSongList(song, currentState.songs)
                                },
                                onLongClick = { onShowSongOptions(song) }
                            )
                        }
                    }
                    if (currentState.songs.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = {
                                playerViewModel.playSongList(
                                    currentState.songs.first(),
                                    currentState.songs
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(
                                    bottom = bottomBarPadding + 16.dp,
                                    end = 16.dp
                                )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play All")
                        }
                    }
                }

                is PlaylistDetailState.Error -> {
                    Text(
                        text = currentState.message,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}