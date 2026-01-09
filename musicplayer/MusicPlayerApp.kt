package com.example.musicplayer

import android.app.Application
import android.content.Intent
import androidx.annotation.OptIn
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.util.UnstableApi
import com.example.musicplayer.data.local.AppDatabase
import com.example.musicplayer.data.repository.QueueRepositoryImpl
import com.example.musicplayer.domain.usecase.QueueUseCase
import com.example.musicplayer.service.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
@UnstableApi
class MusicPlayerApp : Application(), DefaultLifecycleObserver {
    private val appScope = CoroutineScope(Dispatchers.IO)
    companion object {
        private lateinit var queueRepository: QueueRepositoryImpl
        lateinit var queueUseCase: QueueUseCase
            private set
    }
    override fun onCreate() {
        super<Application>.onCreate()
        val database = AppDatabase.getDatabase(this)
        val prefs = getSharedPreferences("queue_prefs", MODE_PRIVATE)
        queueRepository = QueueRepositoryImpl(database.queueDao(), prefs)
        queueUseCase = QueueUseCase(queueRepository)
        appScope.launch {
            queueRepository.loadQueueFromDatabase()
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    @OptIn(UnstableApi::class)
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        val intent = Intent(this, MusicService::class.java)
        intent.action = MusicService.ACTION_APP_FOREGROUND
        startService(intent)
    }
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        appScope.launch {
            queueRepository.saveQueueToDatabase()
        }
    }
    @UnstableApi
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        val intent = Intent(this, MusicService::class.java)
        intent.action = MusicService.ACTION_APP_BACKGROUND
        startService(intent)
        appScope.launch {
            queueRepository.saveQueueToDatabase()
        }
    }
}