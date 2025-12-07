package com.example.musicplayer.ui.screen.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayer.R
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.screen.search.TrackItem
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.playback.QueueViewModel
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun QueueScreen(
    queueViewModel: QueueViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    onShowSongOptions: (Song) -> Unit
) {
    val queue by queueViewModel.queue.observeAsState(initial = emptyList())
    val currentSong by queueViewModel.currentSong.observeAsState()
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        queueViewModel.moveSongInQueue(from.index, to.index)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(queue, key = { it.id }) { song ->
                ReorderableItem(state = reorderableLazyListState, key = song.id) { isDragging ->
                    var isRemoved by remember { mutableStateOf(false) }
                    val updatedCurrentSong by rememberUpdatedState(currentSong)
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                if (song.id != updatedCurrentSong?.id) {
                                    isRemoved = true
                                    true
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        },
                        positionalThreshold = { it * 0.25f }
                    )
                    LaunchedEffect(isRemoved) {
                        if (isRemoved) {
                            delay(300)
                            queueViewModel.remove(song)
                        }
                    }
                    AnimatedVisibility(
                        visible = !isRemoved,
                        exit = shrinkVertically(
                            animationSpec = tween(durationMillis = 300),
                            shrinkTowards = Alignment.Top
                        ) + fadeOut()
                    ) {
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color)
                                )
                            }
                        ) {
                            TrackItem(
                                song = song,
                                onClick = {
                                    if (song.id != currentSong?.id) {
                                        playerViewModel.playSong(song)
                                    }
                                },
                                onLongClick = { onShowSongOptions(song) },
                                modifier = Modifier.padding(horizontal = 8.dp),
                                isPlaying = song.id == updatedCurrentSong?.id,
                                showDragHandle = true,
                                isDragging = isDragging,
                                dragHandleModifier = Modifier.draggableHandle()
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${queue.size} songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = { queueViewModel.shuffle() }) {
                Icon(
                    painter = painterResource(id = R.drawable.shuffle_24px),
                    contentDescription = "Shuffle queue"
                )
            }
        }
    }
}
