package com.example.musicplayer.ui.screen.player

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.musicplayer.viewmodel.playback.PlayerUiState

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
    onShowSongOptions: () -> Unit
) {
    val configuration = LocalConfiguration.current
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        FullPlayerLandscape(
            state = state,
            onPlayPauseClick = onPlayPauseClick,
            onNextClick = onNextClick,
            onPrevClick = onPrevClick,
            onSeek = onSeek,
            onToggleFavorite = onToggleFavorite,
            onToggleRepeat = onToggleRepeat,
            onShowQueue = onShowQueue,
            onShowSleepTimer = onShowSleepTimer,
            onShowSongOptions = onShowSongOptions
        )
    } else {
        FullPlayerPortrait(
            state = state,
            onPlayPauseClick = onPlayPauseClick,
            onNextClick = onNextClick,
            onPrevClick = onPrevClick,
            onSeek = onSeek,
            onToggleFavorite = onToggleFavorite,
            onToggleRepeat = onToggleRepeat,
            onShowQueue = onShowQueue,
            onShowSleepTimer = onShowSleepTimer,
            onShowSongOptions = onShowSongOptions
        )
    }
}
@Composable
private fun FullPlayerLandscape(
    state: PlayerUiState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleRepeat: () -> Unit,
    onShowQueue: () -> Unit,
    onShowSleepTimer: () -> Unit,
    onShowSongOptions: () -> Unit
) {
    val displayTitle = state.title
    val displayArtist = state.artist
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(insets)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            LargeCover(
                currentCoverUrlXL = state.coverUrlXL
            )
        }
        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TrackInfo(
                title = displayTitle,
                artist = displayArtist
            )
            Spacer(modifier = Modifier.height(8.dp))
            Progress(
                position = state.position,
                duration = state.duration,
                onSeek = onSeek
            )
            TrackControl(
                isPlaying = state.isPlaying,
                isLoading = state.isLoading,
                isRepeating = state.isRepeating,
                isFavourite = state.isFavourite,
                onPlayPauseClick = onPlayPauseClick,
                onPrevClick = onPrevClick,
                onNextClick = onNextClick,
                onToggleRepeat = onToggleRepeat,
                onToggleFavorite = onToggleFavorite
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                UpNextBar(
                    upNextSong = state.upNextSong,
                    onShowQueue = onShowQueue,
                    onShowSleepTimer = onShowSleepTimer,
                    onShowSongOptions = onShowSongOptions
                )
            }
        }
    }
}
@Composable
private fun FullPlayerPortrait(
    state: PlayerUiState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleRepeat: () -> Unit,
    onShowQueue: () -> Unit,
    onShowSleepTimer: () -> Unit,
    onShowSongOptions: () -> Unit
) {
    val displayTitle = state.title
    val displayArtist = state.artist
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
                currentCoverUrlXL = state.coverUrlXL
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
                isLoading = state.isLoading,
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