package com.coldstart.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Alarm::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ColdStartDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        fun build(context: Context): ColdStartDatabase =
            Room.databaseBuilder(context, ColdStartDatabase::class.java, "coldstart.db").build()
    }
}
