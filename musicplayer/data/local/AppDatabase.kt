package com.example.musicplayer.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.musicplayer.data.local.history.HistoryDao
import com.example.musicplayer.data.local.history.HistoryEntry
import com.example.musicplayer.data.local.memory.MemoryDao
import com.example.musicplayer.data.local.memory.SongMemory
import com.example.musicplayer.data.local.playlist.FavouriteDao
import com.example.musicplayer.data.local.playlist.FavouriteSong
import com.example.musicplayer.data.local.playlist.PlaylistDao
import com.example.musicplayer.data.local.playlist.PlaylistSongCrossRef
import com.example.musicplayer.data.local.playlist.UserPlaylist
import com.example.musicplayer.data.local.queue.QueueDao
import com.example.musicplayer.data.local.queue.QueueEntry
import com.example.musicplayer.data.local.search.SearchHistoryEntry
import com.example.musicplayer.data.local.search.SearchHistoryDao
import com.example.musicplayer.data.local.song.SongDao
import com.example.musicplayer.domain.model.Song

@Database(
    entities = [
        FavouriteSong::class,
        UserPlaylist::class,
        PlaylistSongCrossRef::class,
        HistoryEntry::class,
        QueueEntry::class,
        SearchHistoryEntry::class,
        Song::class,
        SongMemory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favouriteDao(): FavouriteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun searchQueryDao(): SearchHistoryDao
    abstract fun songDao(): SongDao
    abstract fun memoryDao(): MemoryDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "music_app_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}