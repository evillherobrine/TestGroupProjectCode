package com.example.musicplayer.manager

import android.media.audiofx.Visualizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.hypot
import kotlin.math.max

class AudioVisualizerManager {
    fun getFft(audioSessionId: Int): Flow<List<Float>> = callbackFlow {
        val visualizer = Visualizer(audioSessionId)
        visualizer.captureSize = Visualizer.getCaptureSizeRange()[1]

        val listener = object : Visualizer.OnDataCaptureListener {
            override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) { }

            override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                if (fft != null) {
                    trySend(processFft(fft))
                }
            }
        }
        visualizer.setDataCaptureListener(listener, Visualizer.getMaxCaptureRate() / 2, false, true)
        visualizer.enabled = true
        awaitClose {
            visualizer.enabled = false
            visualizer.release()
        }
    }
    private fun processFft(fft: ByteArray): List<Float> {
        val n = fft.size
        val magnitudes = FloatArray(n / 2)
        val startIndex = 2
        for (k in startIndex until n / 2) {
            val i = k * 2
            val real = fft[i].toFloat()
            val imag = fft[i + 1].toFloat()
            magnitudes[k] = hypot(real, imag) * 4f
        }
        return compressToBars(magnitudes, 30)
    }
    private fun compressToBars(magnitudes: FloatArray, barCount: Int): List<Float> {
        val result = mutableListOf<Float>()
        val blockSize = magnitudes.size / barCount
        for (i in 0 until barCount) {
            val start = i * blockSize
            val end = start + blockSize
            var maxMag = 0f

            for (j in start until end) {
                if (j < magnitudes.size) {
                    maxMag = max(maxMag, magnitudes[j])
                }
            }
            result.add(maxMag)
        }
        return result
    }
}