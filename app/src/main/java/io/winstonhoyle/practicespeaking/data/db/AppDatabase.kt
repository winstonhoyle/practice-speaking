package io.winstonhoyle.practicespeaking.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app has no users to keep data for, so a schema change bumps [version] and the database is
 * rebuilt empty instead of migrated. Write a migration once there are users.
 */
@Database(entities = [SessionEntity::class, MessageEntity::class, MessageFts::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room
                .databaseBuilder(context, AppDatabase::class.java, "practice_speaking.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
