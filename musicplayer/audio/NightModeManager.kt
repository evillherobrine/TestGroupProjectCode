package com.example.musicplayer.audio

import android.media.audiofx.DynamicsProcessing

class NightModeManager {
    private var dynamicsProcessing: DynamicsProcessing?  = null
    private var isEnabled:  Boolean = false
    fun initialize(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            release()
            val channelCount = 2
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                channelCount,
                true,
                1,
                true,
                1,
                true,
                1,
                true
            ).build()
            dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply {
                for (i in 0 until channelCount) {
                    val mbcBand = getMbcBandByChannelIndex(i, 0)
                    mbcBand.apply {
                        isEnabled = true
                        threshold = -30f
                        ratio = 4f
                        attackTime = 10f
                        releaseTime = 200f
                        kneeWidth = 15f
                        noiseGateThreshold = -70f
                        postGain = 5f }
                    setMbcBandByChannelIndex(i, 0, mbcBand)
                    val limiter = getLimiterByChannelIndex(i)
                    limiter.apply {
                        isEnabled = true
                        linkGroup = 0
                        attackTime = 1f
                        releaseTime = 60f
                        ratio = 10f
                        threshold = -1f
                        postGain = 0f }
                    setLimiterByChannelIndex(i, limiter) }
            }
        } catch (e: Exception) {
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