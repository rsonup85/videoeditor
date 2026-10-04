package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.MediaAssetDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.TimelineItemDao
import com.example.data.local.entity.MediaAssetEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.TimelineItemEntity

@Database(
    entities = [
        ProjectEntity::class,
        MediaAssetEntity::class,
        TimelineItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun mediaAssetDao(): MediaAssetDao
    abstract fun timelineItemDao(): TimelineItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vistara_edit_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
