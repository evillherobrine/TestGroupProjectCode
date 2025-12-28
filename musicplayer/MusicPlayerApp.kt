package com.example.musicplayer

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.musicplayer.data.local.AppDatabase
import com.example.musicplayer.data.repository.QueueRepositoryImpl
import com.example.musicplayer.domain.usecase.QueueUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        appScope.launch {
            queueRepository.saveQueueToDatabase()
        }
    }
}