package io.winstonhoyle.practicespeaking.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SessionEntity::class, MessageEntity::class, MessageFts::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "practice_speaking.db").build()
    }
}
