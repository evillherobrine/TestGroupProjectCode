package com.example.musicplayer.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.ui.screen.history.HistoryScreen
import com.example.musicplayer.ui.screen.home.HomeScreenComposable
import com.example.musicplayer.ui.screen.local.LocalDetailScreen
import com.example.musicplayer.ui.screen.local.LocalMusicScreen
import com.example.musicplayer.ui.screen.libary.PlaylistScreenComposable
import com.example.musicplayer.ui.screen.libary.UserPlaylistDetailScreen
import com.example.musicplayer.ui.screen.search.ApiPlaylistDetail
import com.example.musicplayer.ui.screen.search.SearchInputScreen
import com.example.musicplayer.ui.screen.search.SearchResultScreen
import com.example.musicplayer.viewmodel.history.HistoryViewModel
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
import com.example.musicplayer.viewmodel.local.LocalDetailViewModel
import com.example.musicplayer.viewmodel.search.SearchViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable@UnstableApi
fun AppNavHost(
    navController: NavHostController,
    scaffoldPadding: PaddingValues,
    miniPlayerHeight: Dp,
    onShowSongOptions: (song: Song, historyId: Long?, playlistId: Long?, onStartSelection: (() -> Unit)?) -> Unit,
    onShowSnackbar: (String) -> Unit,
    searchViewModel: SearchViewModel,
    scrollToTopHome: Long,
    scrollToTopLibrary: Long,
    scrollToTopLocal: Long,
    scrollToTopHistory: Long
) {
    val layoutDirection = LocalLayoutDirection.current
    val totalBottomPadding = scaffoldPadding.calculateBottomPadding() + miniPlayerHeight
    val screenContentModifier = Modifier.padding(
        bottom = totalBottomPadding,
        start = scaffoldPadding.calculateLeftPadding(layoutDirection),
        end = scaffoldPadding.calculateRightPadding(layoutDirection)
    )
    val playerViewModel: PlayerViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()
    fun isDetailRoute(route: String?): Boolean {
        return route?.startsWith(AppDestinations.API_PLAYLIST_DETAIL) == true ||
                route?.startsWith(AppDestinations.USER_PLAYLIST_DETAIL) == true ||
                route == AppDestinations.SEARCH || route == AppDestinations.HISTORY
    }
    NavHost(
        navController = navController,
        startDestination = AppDestinations.HOME,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            if (isDetailRoute(targetState.destination.route)) {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeIn(tween(300))
            } else {
                fadeIn(tween(300))
            }
        },
        exitTransition = {
            if (isDetailRoute(targetState.destination.route)) {
                fadeOut(tween(300))
            } else {
                fadeOut(tween(300))
            }
        },
        popEnterTransition = {
            fadeIn(tween(300))
        },
        popExitTransition = {
            if (isDetailRoute(initialState.destination.route)) {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeOut(tween(300))
            } else {
                fadeOut(tween(300))
            }
        }
    ) {
        composable(AppDestinations.HOME) {
            HomeScreenComposable(
                onShowSongOptions = { song -> onShowSongOptions(song, null, null, null) },
                modifier = Modifier.fillMaxSize().padding(
                    start = scaffoldPadding.calculateLeftPadding(layoutDirection),
                    end = scaffoldPadding.calculateRightPadding(layoutDirection)
                ),
                playerViewModel = playerViewModel,
                navController = navController,
                showSnackbar = onShowSnackbar,
                scrollToTop = scrollToTopHome,
                bottomPadding = totalBottomPadding
            )
        }
        composable(AppDestinations.LIBRARY) {
            PlaylistScreenComposable(
                onPlaylistClick = { id, name ->
                    if (id == -2L) {
                        navController.navigate(AppDestinations.LOCAL_MUSIC)
                    } else {
                        val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                        navController.navigate("${AppDestinations.USER_PLAYLIST_DETAIL}/$id/$encodedName")
                    }
                },
                modifier = Modifier.fillMaxSize(),
                bottomPadding = totalBottomPadding,
                onShowSnackbar = onShowSnackbar,
                scrollToTop = scrollToTopLibrary,
                navController = navController
            )
        }
        composable(AppDestinations.HISTORY) {
            HistoryScreen(
                scaffoldPadding = scaffoldPadding,
                miniPlayerHeight = miniPlayerHeight,
                onShowSongOptions = { song, historyId -> onShowSongOptions(song, historyId, null, null) },
                playerViewModel = playerViewModel,
                historyViewModel = historyViewModel,
                navController = navController,
                scrollToTop = scrollToTopHistory
            )
        }
        composable(AppDestinations.LOCAL_MUSIC) {
            LocalMusicScreen(
                playerViewModel = playerViewModel,
                onShowSongOptions = { song -> onShowSongOptions(song, null, null, null) },
                onAlbumClick = { id, name ->
                    val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                    navController.navigate("${AppDestinations.LOCAL_ALBUM_DETAIL}/$id/$encodedName")
                },
                onArtistClick = { id, name ->
                    val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                    navController.navigate("${AppDestinations.LOCAL_ARTIST_DETAIL}/$id/$encodedName")
                },
                onFolderClick = { path, name ->
                    val encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                    val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                    navController.navigate("${AppDestinations.LOCAL_FOLDER_DETAIL}/$encodedPath/$encodedName")
                },
                bottomBarPadding = totalBottomPadding,
                scrollToTop = scrollToTopLocal
            )
        }
        composable(AppDestinations.SEARCH) {
            SearchInputScreen(
                searchViewModel = searchViewModel,
                navController = navController,
                modifier = screenContentModifier,
                onSearchSubmit = { query ->
                    val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
                    navController.navigate("${AppDestinations.SEARCH_RESULT}/$encodedQuery")
                }
            )
        }
        composable(
            route = "${AppDestinations.SEARCH_RESULT}/{${AppDestinations.ARG_SEARCH_QUERY}}",
            arguments = listOf(
                navArgument(AppDestinations.ARG_SEARCH_QUERY) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val queryRaw = backStackEntry.arguments?.getString(AppDestinations.ARG_SEARCH_QUERY) ?: ""
            val query = java.net.URLDecoder.decode(queryRaw, "UTF-8")
            SearchResultScreen(
                query = query,
                searchViewModel = searchViewModel,
                playerViewModel = playerViewModel,
                navController = navController,
                onPlaylistClick = { id, name ->
                    val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                    navController.navigate("${AppDestinations.API_PLAYLIST_DETAIL}/$id/$encodedName")
                },
                onShowSongOptions = { song -> onShowSongOptions(song, null, null, null) },
                modifier = screenContentModifier,
                onShowSnackbar = onShowSnackbar
            )
        }
        val detailRouteArgs = listOf(
            navArgument(AppDestinations.ARG_PLAYLIST_ID) { type = NavType.LongType },
            navArgument(AppDestinations.ARG_PLAYLIST_NAME) { type = NavType.StringType }
        )
        composable(
            route = "${AppDestinations.API_PLAYLIST_DETAIL}/{${AppDestinations.ARG_PLAYLIST_ID}}/{${AppDestinations.ARG_PLAYLIST_NAME}}",
            arguments = detailRouteArgs
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(AppDestinations.ARG_PLAYLIST_ID) ?: 0L
            val name = backStackEntry.arguments?.getString(AppDestinations.ARG_PLAYLIST_NAME) ?: "Playlist"
            val decodedName = java.net.URLDecoder.decode(name, "UTF-8")
            ApiPlaylistDetail(
                playlistId = id,
                playlistName = decodedName,
                onNavigateBack = { navController.popBackStack() },
                onShowSongOptions = { song -> onShowSongOptions(song, null, null, null) },
                bottomBarPadding = totalBottomPadding,
                playerViewModel = playerViewModel
            )
        }
        composable(
            route = "${AppDestinations.USER_PLAYLIST_DETAIL}/{${AppDestinations.ARG_PLAYLIST_ID}}/{${AppDestinations.ARG_PLAYLIST_NAME}}",
            arguments = detailRouteArgs
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(AppDestinations.ARG_PLAYLIST_ID) ?: 0L
            val name = backStackEntry.arguments?.getString(AppDestinations.ARG_PLAYLIST_NAME) ?: "Playlist"
            val decodedName = java.net.URLDecoder.decode(name, "UTF-8")
            UserPlaylistDetailScreen(
                playlistId = id,
                playlistName = decodedName,
                playerViewModel = playerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onShowSongOptions = { song, onStartSelection -> onShowSongOptions(song, null, id, onStartSelection) },
                bottomBarPadding = totalBottomPadding
            )
        }
        composable(
            route = "${AppDestinations.LOCAL_ALBUM_DETAIL}/{${AppDestinations.ARG_ID}}/{${AppDestinations.ARG_NAME}}",
            arguments = listOf(
                navArgument(AppDestinations.ARG_ID) { type = NavType.LongType },
                navArgument(AppDestinations.ARG_NAME) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(AppDestinations.ARG_ID) ?: 0L
            val name = backStackEntry.arguments?.getString(AppDestinations.ARG_NAME) ?: ""
            val decodedName = java.net.URLDecoder.decode(name, "UTF-8")

            LocalDetailScreen(
                title = decodedName,
                type = LocalDetailViewModel.DetailType.ALBUM,
                id = id,
                playerViewModel = playerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onShowSongOptions = { song -> onShowSongOptions(song, null, null, null) },
                bottomBarPadding = totalBottomPadding
            )
        }
        composable(
            route = "${AppDestinations.LOCAL_ARTIST_DETAIL}/{${AppDestinations.ARG_ID}}/{${AppDestinations.ARG_NAME}}",
            arguments = listOf(
                navArgument(AppDestinations.ARG_ID) { type = NavType.LongType },
                navArgument(AppDestinations.ARG_NAME) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(AppDestinations.ARG_ID) ?: 0L
            val name = backStackEntry.arguments?.getString(AppDestinations.ARG_NAME) ?: ""
            val decodedName = java.net.URLDecoder.decode(name, "UTF-8")

            LocalDetailScreen(
                title = decodedName,
                type = LocalDetailViewModel.DetailType.ARTIST,
                id = id,
                playerViewModel = playerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onShowSongOptions = { song -> onShowSongOptions(song, null, null, null) },
                bottomBarPadding = totalBottomPadding
            )
        }
        composable(
            route = "${AppDestinations.LOCAL_FOLDER_DETAIL}/{${AppDestinations.ARG_ID}}/{${AppDestinations.ARG_NAME}}",
            arguments = listOf(
                navArgument(AppDestinations.ARG_ID) { type = NavType.StringType },
                navArgument(AppDestinations.ARG_NAME) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pathRaw = backStackEntry.arguments?.getString(AppDestinations.ARG_ID) ?: ""
            val nameRaw = backStackEntry.arguments?.getString(AppDestinations.ARG_NAME) ?: ""
            val decodedPath = java.net.URLDecoder.decode(pathRaw, "UTF-8")
            val decodedName = java.net.URLDecoder.decode(nameRaw, "UTF-8")
            LocalDetailScreen(
                title = decodedName,
                type = LocalDetailViewModel.DetailType.FOLDER,
                id = 0,
                path = decodedPath,
                playerViewModel = playerViewModel,
                onNavigateBack = { navController.popBackStack() },
                onShowSongOptions = { song -> onShowSongOptions(song, null, null, null) },
                bottomBarPadding = totalBottomPadding
            )
        }
    }
}