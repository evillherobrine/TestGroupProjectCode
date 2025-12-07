package com.example.musicplayer.ui.screen.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.viewmodel.playback.PlayerUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullPlayer(
    state: PlayerUiState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleRepeat: () -> Unit,
    onShowQueue: () -> Unit,
    onShowSleepTimer: () -> Unit,
    onShowSongOptions: () -> Unit,
    onSongSwipe: (Song) -> Unit
) {
    val initialPage = if (state.currentIndex != -1 && state.currentIndex < state.queue.size) state.currentIndex else 0
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { if (state.queue.isNotEmpty()) state.queue.size else 1 }
    )
    val currentState by rememberUpdatedState(state)
    val currentOnSongSwipe by rememberUpdatedState(onSongSwipe)
    LaunchedEffect(state.currentIndex) {
        if (state.currentIndex != -1 && state.currentIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(state.currentIndex)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (currentState.queue.isNotEmpty() &&
                page != currentState.currentIndex &&
                page in currentState.queue.indices
            ) {
                currentOnSongSwipe(currentState.queue[page])
            }
        }
    }
    val displaySong = if (state.queue.isNotEmpty() && pagerState.currentPage in state.queue.indices) {
        state.queue[pagerState.currentPage]
    } else {
        state.currentSong
    }
    val displayTitle = displaySong?.title ?: state.title
    val displayArtist = displaySong?.artist ?: state.artist
    val topPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = topPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            LargeCover(
                queue = state.queue,
                currentCoverUrlXL = state.coverUrlXL,
                pagerState = pagerState
            )
            TrackInfo(
                title = displayTitle,
                artist = displayArtist
            )
            Progress(
                position = state.position,
                duration = state.duration,
                onSeek = onSeek
            )
            TrackControl(
                isPlaying = state.isPlaying,
                isRepeating = state.isRepeating,
                isFavourite = state.isFavourite,
                onPlayPauseClick = onPlayPauseClick,
                onPrevClick = onPrevClick,
                onNextClick = onNextClick,
                onToggleRepeat = onToggleRepeat,
                onToggleFavorite = onToggleFavorite
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            UpNextBar(
                upNextSong = state.upNextSong,
                onShowQueue = onShowQueue,
                onShowSleepTimer = onShowSleepTimer,
                onShowSongOptions = onShowSongOptions
            )
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}