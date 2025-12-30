package com.example.musicplayer.ui.screen.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.viewmodel.playback.PlayerUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScaffold(
    playerState: PlayerUiState,
    scaffoldState: BottomSheetScaffoldState,
    snackbarHostState: SnackbarHostState,
    bottomBarHeight: Dp,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleRepeat: () -> Unit,
    onShowQueue: () -> Unit,
    onShowSleepTimer: () -> Unit,
    onShowSongOptions: (Song?) -> Unit,
    content: @Composable (PaddingValues, Dp) -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val systemNavBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val peekHeight = MiniPlayerHeight + bottomBarHeight + systemNavBarHeight
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        sheetDragHandle = null,
        sheetShape = RectangleShape,
        sheetContainerColor = Color.Transparent,
        sheetPeekHeight = peekHeight,
        sheetMaxWidth = Dp.Unspecified,
        sheetContent = {
            BackHandler(enabled = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded) {
                scope.launch { scaffoldState.bottomSheetState.partialExpand() }
            }
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val fullHeight = maxHeight
                val progress by remember(scaffoldState.bottomSheetState) {
                    derivedStateOf {
                        try {
                            val offset = scaffoldState.bottomSheetState.requireOffset()
                            val maxOffset = with(density) { (fullHeight - peekHeight).toPx() }
                            if (maxOffset > 0) {
                                (1f - (offset / maxOffset)).coerceIn(0f, 1f)
                            } else 0f
                        } catch (_: Exception) {
                            if (scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded) 1f else 0f
                        }
                    }
                }
                val fullPlayerZIndex = if (progress >= 0.5f) 1f else 0f
                val miniPlayerZIndex = if (progress < 0.5f) 1f else 0f
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(fullPlayerZIndex)
                        .graphicsLayer {
                            alpha = progress
                        }
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                        FullPlayer(
                            state = playerState,
                            onPlayPauseClick = onPlayPause,
                            onNextClick = onNext,
                            onPrevClick = onPrev,
                            onSeek = onSeek,
                            onToggleFavorite = onToggleFavorite,
                            onToggleRepeat = onToggleRepeat,
                            onShowQueue = onShowQueue,
                            onShowSleepTimer = onShowSleepTimer,
                            onShowSongOptions = { onShowSongOptions(playerState.currentSong) }
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .zIndex(miniPlayerZIndex)
                        .graphicsLayer {
                            alpha = 1f - progress
                        }
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        MiniPlayer(
                            state = playerState,
                            onPlayPauseClick = onPlayPause,
                            onMiniPlayerClick = {
                                scope.launch { scaffoldState.bottomSheetState.expand() }
                            },
                            modifier = Modifier.height(MiniPlayerHeight)
                        )
                    }
                    Spacer(modifier = Modifier.height(bottomBarHeight + systemNavBarHeight))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            content(PaddingValues(0.dp), MiniPlayerHeight)
        }
    }
}