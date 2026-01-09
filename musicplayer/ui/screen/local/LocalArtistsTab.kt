package com.example.musicplayer.ui.screen.local

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.musicplayer.ui.screen.component.SortHeader
import com.example.musicplayer.viewmodel.local.LocalMusicViewModel

@Composable
fun LocalArtistsTab(
    viewModel: LocalMusicViewModel,
    onArtistClick: (Long, String) -> Unit,
    bottomPadding: Dp,
    scrollToTop: Long
) {
    val pagingItems = viewModel.localArtistsFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val sortOption by viewModel.artistSortOption.collectAsState()
    val sortDirection by viewModel.artistSortDirection.collectAsState()
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
            itemLabel = "artists",
            onSortOptionSelected = { viewModel.updateArtistSortOption(it) },
            onSortDirectionToggle = { viewModel.toggleArtistSortDirection() }
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = bottomPadding)
        ) {
            items(pagingItems.itemCount) { index ->
                pagingItems[index]?.let { artist ->
                    ListItem(
                        modifier = Modifier.clickable { onArtistClick(artist.id, artist.name) },
                        headlineContent = { Text(artist.name) },
                        supportingContent = { Text("${artist.trackCount} songs") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            )
                        }
                    )
                }
            }
        }
    }
}