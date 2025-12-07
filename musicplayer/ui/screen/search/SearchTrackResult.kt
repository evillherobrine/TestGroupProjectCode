package com.example.musicplayer.ui.screen.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.R
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.utils.RippleHighlightItem
import com.example.musicplayer.viewmodel.search.SearchViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue

@Composable
fun TrackResultScreen(
    viewModel: SearchViewModel,
    onTrackClick: (Song) -> Unit,
    onTrackLongClick: (Song) -> Unit
) {
    val lazyTrackItems = viewModel.tracksFlow.collectAsLazyPagingItems()
    val loadState = lazyTrackItems.loadState
    val committedQuery by viewModel.query.collectAsState()
    LaunchedEffect(loadState, committedQuery) {
        if (loadState.refresh is LoadState.NotLoading && committedQuery.isNotEmpty()) {
            val itemCount = lazyTrackItems.itemCount
            if (itemCount > 0) {
                val hasEnough = itemCount >= 4
                viewModel.saveSuccessfulQuery(committedQuery, hasEnough)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            items(
                count = lazyTrackItems.itemCount,
                key = { index -> lazyTrackItems.peek(index)?.id ?: -1 }
            ) { index ->
                lazyTrackItems[index]?.let { song ->
                    TrackItem(
                        song = song,
                        onClick = { onTrackClick(song) },
                        onLongClick = { onTrackLongClick(song) }
                    )
                }
            }
        }
        if (lazyTrackItems.loadState.refresh is LoadState.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun TrackItem(
    song: Song,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    showDragHandle: Boolean = false,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier = Modifier
) {
    RippleHighlightItem(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (isDragging) 4.dp else 0.dp)
            .clip(MaterialTheme.shapes.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.cover)
                    .crossfade(true)
                    .placeholder(R.drawable.image_24px)
                    .error(R.drawable.image_24px)
                    .build(),
                contentDescription = "Song Cover",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (isPlaying) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Now playing",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Now playing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                } else {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (showDragHandle) {
                Icon(
                    painter = painterResource(id = R.drawable.drag_handle_24px),
                    contentDescription = "Drag to reorder",
                    modifier = Modifier
                        .then(dragHandleModifier)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(12.dp)
                        .size(24.dp)
                )
            }
        }
    }
}