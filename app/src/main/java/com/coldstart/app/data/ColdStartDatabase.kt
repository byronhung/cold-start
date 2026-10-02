package com.coldstart.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Alarm::class, WakeLog::class, RoundResult::class, AppSettings::class], version = 3, exportSchema = false)
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }

        /** v3 (v0.2): wake checks, scan-to-finish, and the shared wake-up code. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `alarms` ADD COLUMN `wakeChecks` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `alarms` ADD COLUMN `finishWithScan` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `wake_log` ADD COLUMN `checksPassed` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `wake_log` ADD COLUMN `checksMissed` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS `settings` (`id` INTEGER NOT NULL, `wakeCode` TEXT, PRIMARY KEY(`id`))")
                // Difficulty restarts at the new top level: v0.1's results were on a 3-round,
                // level-2 scale and would hold every type down at 2.
                db.execSQL("DELETE FROM `round_results`")
            }
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
