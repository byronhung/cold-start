package com.coldstart.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Alarm::class, WakeLog::class, RoundResult::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ColdStartDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun wakeDao(): WakeDao

    companion object {
        private const val NAME = "coldstart.db"

        /**
         * Lives in *device-protected* storage, which Android can read before the first unlock after
         * a reboot. Normal storage stays encrypted until then, so a phone that restarts at 3am
         * (Samsung's auto-restart does exactly this) couldn't reschedule a 6:30 alarm.
         */
        fun build(context: Context): ColdStartDatabase {
            val deviceContext = context.createDeviceProtectedStorageContext()
            // One-time move from normal storage, where chunk 02 created it. No-op once moved;
            // fails harmlessly if the phone is still locked.
            runCatching { deviceContext.moveDatabaseFrom(context, NAME) }
            return Room.databaseBuilder(deviceContext, ColdStartDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        /** v2 adds the wake history and per-round results. The alarms table is unchanged. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `wake_log` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`alarmId` INTEGER NOT NULL, " +
                        "`firedAt` INTEGER NOT NULL, " +
                        "`endedAt` INTEGER, " +
                        "`outcome` TEXT, " +
                        "`opener` TEXT)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `round_results` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`wakeId` INTEGER NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`level` INTEGER NOT NULL, " +
                        "`solveMs` INTEGER NOT NULL, " +
                        "`misses` INTEGER NOT NULL)",
                )
            }
        }
    }
}
