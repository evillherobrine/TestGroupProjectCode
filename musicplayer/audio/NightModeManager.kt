package com.example.musicplayer.audio

import android.media.audiofx.DynamicsProcessing

class NightModeManager {
    private var dynamicsProcessing: DynamicsProcessing?  = null
    private var isEnabled:  Boolean = false
    fun initialize(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            release()
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                1,
                true,
                1,
                true,
                1,
                true,
                1,
                true
            ).build()
            dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply {
                val mbcBand = getMbcBandByChannelIndex(0, 0)
                mbcBand.apply {
                    threshold = -30f
                    ratio = 4f
                    attackTime = 5f
                    releaseTime = 100f
                    kneeWidth = 10f
                    noiseGateThreshold = -60f
                    postGain = 6f
                    isEnabled = true }
                setMbcBandByChannelIndex(0, 0, mbcBand)
                val limiter = getLimiterByChannelIndex(0)
                limiter.apply {
                    isEnabled = true
                    linkGroup = 0
                    attackTime = 1f
                    releaseTime = 50f
                    ratio = 10f
                    threshold = -1f
                    postGain = 0f}
                setLimiterByChannelIndex(0, limiter)
            }
        } catch (e:Exception) {
            e.printStackTrace()
            dynamicsProcessing = null
        }
    }
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        dynamicsProcessing?.enabled = enabled
    }
    fun toggle(): Boolean {
        isEnabled = !isEnabled
        android.util.Log.d("NightMode", "NightMode: $isEnabled")
        try {
            dynamicsProcessing?.enabled = isEnabled
        } catch (e: Exception) {
            android.util.Log.e("NightMode", "NightMode: ${e.message}")
        }
        return isEnabled
    }
    fun isNightModeEnabled(): Boolean = isEnabled
    fun release() {
        try {
            dynamicsProcessing?.release()
        } catch (e:  Exception) {
            e.printStackTrace()
        }
        dynamicsProcessing = null
        isEnabled = false
    }
    fun updateAudioSession(audioSessionId:  Int) {
        val wasEnabled = isEnabled
        initialize(audioSessionId)
        if (wasEnabled) {
            setEnabled(true)
        }
    }
}