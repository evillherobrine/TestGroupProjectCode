package com.example.musicplayer.ui.screen.local

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.musicplayer.viewmodel.local.LocalMusicViewModel

@Composable
fun LocalFoldersTab(
    viewModel: LocalMusicViewModel,
    onFolderClick: (String, String) -> Unit,
    bottomPadding: Dp,
    scrollToTop: Long
) {
    val pagingItems = viewModel.localFoldersFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0) {
            listState.animateScrollToItem(0)
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding)
    ) {
        items(pagingItems.itemCount) { index ->
            pagingItems[index]?.let { folder ->
                ListItem(
                    modifier = Modifier.clickable { onFolderClick(folder.path, folder.name) },
                    headlineContent = { Text(folder.name) },
                    supportingContent = { Text("${folder.songCount} songs") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                )
            }
        }
    }
}