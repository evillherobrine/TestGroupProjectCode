package com.example.musicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.example.musicplayer.ui.theme.AppTheme
import com.example.musicplayer.ui.screen.component.MainScreen
import com.example.musicplayer.viewmodel.playback.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
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