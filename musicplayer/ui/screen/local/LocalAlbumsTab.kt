package com.example.musicplayer.ui.screen.local

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.R
import com.example.musicplayer.ui.screen.component.SortHeader
import com.example.musicplayer.viewmodel.local.LocalMusicViewModel

@Composable
fun LocalAlbumsTab(
    viewModel: LocalMusicViewModel,
    onAlbumClick: (Long, String) -> Unit,
    bottomPadding: Dp,
    scrollToTop: Long
) {
    val pagingItems = viewModel.localAlbumsFlow.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    val sortOption by viewModel.albumSortOption.collectAsState()
    val sortDirection by viewModel.albumSortDirection.collectAsState()
    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0) {
            gridState.animateScrollToItem(0)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        SortHeader(
            sortOption = sortOption,
            sortDirection = sortDirection,
            itemCount = pagingItems.itemCount,
            itemLabel = "albums",
            onSortOptionSelected = { viewModel.updateAlbumSortOption(it) },
            onSortDirectionToggle = { viewModel.toggleAlbumSortDirection() }
        )
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(140.dp),
            contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, bottomPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(pagingItems.itemCount) { index ->
                pagingItems[index]?.let { album ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(140.dp)
                            .clickable { onAlbumClick(album.id, album.name) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(album.coverUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit,
                            error = painterResource(R.drawable.image_24px)
                        )
                        Text(
                            text = album.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = album.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}