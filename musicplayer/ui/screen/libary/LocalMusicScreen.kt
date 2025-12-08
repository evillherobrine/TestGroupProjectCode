package com.example.musicplayer.ui.screen.libary

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.screen.search.TrackItem
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.playlist.LocalMusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable@UnstableApi
fun LocalMusicScreen(
    playerViewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    onShowSongOptions: (Song) -> Unit,
    bottomBarPadding: Dp,
    viewModel: LocalMusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val pagingItems = viewModel.localMusicFlow.collectAsLazyPagingItems()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermissionStatus(isGranted)
        if (isGranted) {
            pagingItems.refresh()
        }
    }
    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            permissionToRequest
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updatePermissionStatus(isGranted)
        if (!isGranted) {
            permissionLauncher.launch(permissionToRequest)
        }
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Local Music")
                        if (hasPermission && pagingItems.itemCount > 0) {
                            Text(
                                text = "${pagingItems.itemCount} songs",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (!hasPermission) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Permission Required",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "This app needs access to your audio files to play local music. Please grant the permission to continue.",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("Open Settings")
                    }
                }
            } else {
                if (pagingItems.loadState.refresh is LoadState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                else if (pagingItems.itemCount == 0 && pagingItems.loadState.refresh is LoadState.NotLoading) {
                    Text(
                        text = "No music files found on device",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = bottomBarPadding)
                    ) {
                        items(
                            count = pagingItems.itemCount,
                            key = pagingItems.itemKey { it.id },
                            contentType = pagingItems.itemContentType { "song" }
                        ) { index ->
                            val song = pagingItems[index]
                            if (song != null) {
                                TrackItem(
                                    song = song,
                                    onClick = {
                                        playerViewModel.playSongList(song, pagingItems.itemSnapshotList.items)
                                    },
                                    onLongClick = { onShowSongOptions(song) }
                                )
                            }
                        }
                        if (pagingItems.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                    if (pagingItems.itemCount > 0) {
                        FloatingActionButton(
                            onClick = {
                                pagingItems.itemSnapshotList.items.firstOrNull()?.let { firstSong ->
                                    playerViewModel.playSongList(firstSong, pagingItems.itemSnapshotList.items)
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(
                                    bottom = bottomBarPadding + 16.dp,
                                    end = 16.dp
                                )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play All")
                        }
                    }
                }
            }
        }
    }
}