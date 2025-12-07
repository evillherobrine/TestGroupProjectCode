package com.example.musicplayer

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.musicplayer.data.repository.QueueRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MusicPlayerApp : Application(), DefaultLifecycleObserver {
    private val appScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super<Application>.onCreate()
        QueueRepositoryImpl.initialize(this)
        appScope.launch {
            QueueRepositoryImpl.loadQueueFromDatabase()
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        appScope.launch {
            QueueRepositoryImpl.saveQueueToDatabase()
        }
    }
}