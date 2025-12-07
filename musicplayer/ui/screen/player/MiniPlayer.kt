package com.example.musicplayer.ui.screen.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.R
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.viewmodel.playback.PlayerUiState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith

val MiniPlayerHeight: Dp = 64.dp

@Composable
fun MiniPlayer(
    state: PlayerUiState,
    onPlayPauseClick: () -> Unit,
    onMiniPlayerClick: () -> Unit,
    onSongSwipe: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentState by rememberUpdatedState(state)
    val currentOnSongSwipe by rememberUpdatedState(onSongSwipe)
    var targetIndex by remember { mutableIntStateOf(state.currentIndex) }
    val initialPage = if (state.currentIndex != -1 && state.currentIndex < state.queue.size) state.currentIndex else 0
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { if (state.queue.isNotEmpty()) state.queue.size else 1 }
    )
    LaunchedEffect(state.currentIndex) {
        if (state.currentIndex != -1 && state.currentIndex != targetIndex) {
            targetIndex = state.currentIndex
            if (pagerState.currentPage != state.currentIndex && !pagerState.isScrollInProgress) {
                pagerState.scrollToPage(state.currentIndex)
            }
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (currentState.queue.isNotEmpty() && page in currentState.queue.indices) {
                if (page != targetIndex) {
                    targetIndex = page
                    currentOnSongSwipe(currentState.queue[page])
                }
            }
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            .clickable { onMiniPlayerClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (state.queue.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) { page ->
                    val song = state.queue.getOrNull(page)
                    if (song != null) {
                        MiniPlayerContent(
                            title = song.title,
                            artist = song.artist,
                            coverUrl = song.cover ?: ""
                        )
                    }
                }
            } else {
                MiniPlayerContent(
                    title = state.title,
                    artist = state.artist,
                    coverUrl = state.coverUrl
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        IconButton(onClick = onPlayPauseClick) {
            AnimatedContent(
                targetState = state.isPlaying,
                transitionSpec = {
                    (scaleIn(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)))
                        .togetherWith(scaleOut(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200)))
                },
                label = "PlayPauseAnimation"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}
@Composable
private fun MiniPlayerContent(
    title: String,
    artist: String,
    coverUrl: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(coverUrl)
                .crossfade(true)
                .placeholder(R.drawable.image_24px)
                .error(R.drawable.image_24px)
                .build(),
            contentDescription = "Album Cover",
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}