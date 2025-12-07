package com.example.musicplayer.ui.screen.search

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.screen.component.PlaylistOptionsSheet
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.playback.QueueViewModel
import com.example.musicplayer.viewmodel.playlist.LibraryViewModel
import com.example.musicplayer.viewmodel.search.SearchViewModel
import kotlinx.coroutines.launch

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchResultScreen(
    query: String,
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel,
    playerViewModel: PlayerViewModel,
    navController: NavController,
    onPlaylistClick: (Long, String) -> Unit,
    onShowSongOptions: (Song) -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val context = LocalContext.current.applicationContext as Application
    val libraryViewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory(context))
    val queueViewModel: QueueViewModel = viewModel()
    LaunchedEffect(query) {
        searchViewModel.setQuery(query)
        searchViewModel.onQueryChange(query)
    }
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showPlaylistOptions by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable {
                                navController.popBackStack()
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = query,
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            val tabTitles = listOf("Songs", "Playlists")
            val pagerState = rememberPagerState(initialPage = 0) { tabTitles.size }
            SecondaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) }
                    )
                }
            }
            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> TrackResultScreen(
                        viewModel = searchViewModel,
                        onTrackClick = { playerViewModel.playSong(it) },
                        onTrackLongClick = onShowSongOptions
                    )
                    1 -> PlaylistResultScreen(
                        viewModel = searchViewModel,
                        onPlaylistClick = { onPlaylistClick(it.id, it.name) },
                        onPlaylistLongClick = {
                            selectedPlaylist = it
                            showPlaylistOptions = true
                        }
                    )
                }
            }
        }
    }
    if (showPlaylistOptions && selectedPlaylist != null) {
        ModalBottomSheet(
            onDismissRequest = { showPlaylistOptions = false },
            sheetState = sheetState,
            dragHandle = null
        ) {
            PlaylistOptionsSheet(
                playlist = selectedPlaylist!!,
                isLocalPlaylist = false,
                onDismiss = { coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { showPlaylistOptions = false } },
                onPlayPlaylist = {
                    selectedPlaylist?.let { pl ->
                        libraryViewModel.fetchOnlineSongs(pl.id) { if (it.isNotEmpty()) playerViewModel.playSongList(it.first(), it) }
                    }
                },
                onAddToQueue = {
                    selectedPlaylist?.let { pl -> libraryViewModel.fetchOnlineSongs(pl.id) { s -> s.forEach { queueViewModel.add(it) } } }
                },
                onSavePlaylist = {
                    selectedPlaylist?.let { pl -> libraryViewModel.importOnlinePlaylist(pl.id, pl.name); onShowSnackbar("Saved") }
                }
            )
        }
    }
}