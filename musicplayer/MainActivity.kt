package com.example.musicplayer

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import com.example.musicplayer.ui.theme.AppTheme
import com.example.musicplayer.ui.screen.component.MainScreen
import com.example.musicplayer.viewmodel.playback.PlayerViewModel
@UnstableApi
class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val configuration = LocalConfiguration.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val context = LocalContext.current
            val density = LocalDensity.current
            val isImeVisible = WindowInsets.ime.getBottom(density) > 0
            fun updateSystemBars() {
                val activity = context as? Activity
                val window = activity?.window ?: return
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    if (!isImeVisible) {
                        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
                        insetsController.show(WindowInsetsCompat.Type.statusBars())
                        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } else {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }
            LaunchedEffect(configuration.orientation, isImeVisible) {
                updateSystemBars()
            }
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        updateSystemBars()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            AppTheme {
                MainScreen()
            }
        }
    }
    override fun onStart() {
        super.onStart()
        playerViewModel.requestInitialState()
    }

    override fun onStop() {
        super.onStop()
    }
}