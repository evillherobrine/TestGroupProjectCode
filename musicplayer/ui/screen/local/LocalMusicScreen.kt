package com.example.musicplayer.ui.screen.local

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.viewmodel.local.LocalMusicViewModel
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
@UnstableApi
fun LocalMusicScreen(
    playerViewModel: PlayerViewModel,
    onShowSongOptions: (Song) -> Unit,
    onAlbumClick: (Long, String) -> Unit,
    onArtistClick: (Long, String) -> Unit,
    onFolderClick: (String, String) -> Unit, // Callback nhận path và name
    bottomBarPadding: Dp,
    scrollToTop: Long,
    viewModel: LocalMusicViewModel = viewModel()
) {
    val context = LocalContext.current
    val hasPermission by viewModel.hasPermission.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scope = rememberCoroutineScope()

    // 1. Thêm Tab "Folders"
    val tabTitles = listOf("Songs", "Albums", "Artists", "Folders")
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermissionStatus(isGranted)
    }

    // 2. Launcher mở trình chọn thư mục
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Cấp quyền đọc bền vững cho URI này để dùng lại sau này
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)

            // Lấy tên thư mục để hiển thị (lấy đoạn cuối của path)
            val folderName = uri.path?.split(":")?.lastOrNull()?.split("/")?.lastOrNull() ?: "Selected Folder"

            // Chuyển sang màn hình chi tiết
            onFolderClick(uri.toString(), folderName)
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
            Column {
                TopAppBar(
                    title = { Text("Local Music") },
                    scrollBehavior = scrollBehavior
                )
                if (hasPermission) {
                    SecondaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                text = { Text(title) }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (!hasPermission) {
                PermissionRequestScreen(context = context)
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> LocalSongsTab(
                            viewModel = viewModel,
                            playerViewModel = playerViewModel,
                            onShowSongOptions = onShowSongOptions,
                            bottomPadding = bottomBarPadding,
                            scrollToTop = scrollToTop
                        )
                        1 -> LocalAlbumsTab(
                            viewModel = viewModel,
                            onAlbumClick = onAlbumClick,
                            bottomPadding = bottomBarPadding,
                            scrollToTop = scrollToTop
                        )
                        2 -> LocalArtistsTab(
                            viewModel = viewModel,
                            onArtistClick = onArtistClick,
                            bottomPadding = bottomBarPadding,
                            scrollToTop = scrollToTop
                        )
                        3 -> LocalFoldersTab(
                            onOpenFolderClick = { folderPickerLauncher.launch(null) }
                        )
                    }
                }
            }
        }
    }
}