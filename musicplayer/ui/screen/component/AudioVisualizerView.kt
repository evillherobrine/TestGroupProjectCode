package com.example.musicplayer.ui.screen.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.musicplayer.manager.AudioVisualizerManager

@Composable
fun AudioVisualizerView(
    audioSessionId: Int,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val manager = remember { AudioVisualizerManager() }
    var fftBars by remember { mutableStateOf<List<Float>>(emptyList()) }
    LaunchedEffect(audioSessionId, isExpanded) {
        if (isExpanded && audioSessionId != 0) {
            manager.getFft(audioSessionId).collect { newBars ->
                fftBars = newBars
            }
        } else {
            fftBars = emptyList()
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        if (fftBars.isNotEmpty()) {
            val width = size.width
            val height = size.height
            val maxWaveHeight = height * 0.75f
            val count = fftBars.size
            val gap = 8f
            val barWidth = (width - (count - 1) * gap) / count
            val brush = Brush.verticalGradient(
                colors = listOf(barColor.copy(alpha = 0.5f), barColor))
            fftBars.forEachIndexed { index, magnitude ->
                val scaledHeight = (magnitude / 128f * maxWaveHeight).coerceAtMost(maxWaveHeight)
                val drawHeight = maxOf(scaledHeight, 10f)
                val x = index * (barWidth + gap)
                val y = height - drawHeight
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, drawHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
    }
}