package com.example.coling.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, TimelineClipEntity::class, ColorNodeEntity::class, MediaAssetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ColingDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun timelineClipDao(): TimelineClipDao
    abstract fun colorNodeDao(): ColorNodeDao
    abstract fun mediaAssetDao(): MediaAssetDao

    companion object {
        @Volatile
        private var INSTANCE: ColingDatabase? = null

        fun getDatabase(context: Context): ColingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ColingDatabase::class.java,
                    "coling_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
