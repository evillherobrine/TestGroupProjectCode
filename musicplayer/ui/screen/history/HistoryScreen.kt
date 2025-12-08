package com.example.musicplayer.ui.screen.history

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.screen.search.TrackItem
import com.example.musicplayer.viewmodel.history.HistoryViewModel
import com.example.musicplayer.viewmodel.playback.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable@UnstableApi
fun HistoryScreen(
    scaffoldPadding: PaddingValues,
    miniPlayerHeight: Dp,
    onShowSongOptions: (Song, Long?) -> Unit,
    playerViewModel: PlayerViewModel,
    historyViewModel: HistoryViewModel,
    navController: NavController,
    scrollToTop: Long
) {
    val history by historyViewModel.history.collectAsState()
    val layoutDirection = LocalLayoutDirection.current
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current
    val showClearDialog = remember { mutableStateOf(false) }
    LaunchedEffect(scrollToTop) {
        if (scrollToTop > 0) {
            listState.animateScrollToItem(0)
        }
    }
    if (showClearDialog.value) {
        AlertDialog(
            onDismissRequest = { showClearDialog.value = false },
            title = { Text("Delete history") },
            text = { Text("Your listening history will be permanently removed") },
            confirmButton = {
                TextButton(
                    onClick = {
                        historyViewModel.clearAll()
                        showClearDialog.value = false
                    }
                ) { Text("Accept") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog.value = false }) { Text("Cancel") }
            }
        )
    }
    Scaffold(
        modifier = Modifier
            .padding(
                start = scaffoldPadding.calculateStartPadding(layoutDirection),
                end = scaffoldPadding.calculateEndPadding(layoutDirection),
                bottom = scaffoldPadding.calculateBottomPadding() + miniPlayerHeight
            )
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("History", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (history.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "History is empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                showClearDialog.value = true
                            }
                        }
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = "Clear history")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(history, key = { it.id }) { historyEntry ->
                val song = historyEntry.toSong()
                TrackItem(
                    song = song,
                    onClick = { playerViewModel.playSong(song) },
                    onLongClick = { onShowSongOptions(song, historyEntry.id) }
                )
            }
        }
    }
}