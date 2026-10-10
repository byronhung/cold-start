package com.byronhung.firstlight.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Alarm::class, WakeLog::class, RoundResult::class, AppSettings::class], version = 11, exportSchema = false)
@TypeConverters(Converters::class)
abstract class FirstLightDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun wakeDao(): WakeDao

    companion object {
        private const val NAME = "firstlight.db"

        /**
         * Lives in *device-protected* storage, which Android can read before the first unlock after
         * a reboot. Normal storage stays encrypted until then, so a phone that restarts at 3am
         * (Samsung's auto-restart does exactly this) couldn't reschedule a 6:30 alarm.
         */
        fun build(context: Context): FirstLightDatabase {
            val deviceContext = context.createDeviceProtectedStorageContext()
            // One-time move from normal storage, where chunk 02 created it. No-op once moved;
            // fails harmlessly if the phone is still locked.
            runCatching { deviceContext.moveDatabaseFrom(context, NAME) }
            return Room.databaseBuilder(deviceContext, FirstLightDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .build()
        }

        /** v11: Appearance (by the hour, light, dark, match phone). */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `appearance` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v10: gentle start per alarm (on for every existing alarm too), and skip next. */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `alarms` ADD COLUMN `gentleStart` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `alarms` ADD COLUMN `skipAt` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v9: the first-run welcome, and the Plus popup's timing. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `welcomeDone` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `plusNudgeAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `plusNudgeDismissals` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v8: the sky theme. Everyone starts on Sunrise. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `theme` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v7: puzzle mix and default difficulty move to Settings, plus the Plus flag. Alarms that were
         * Normal now follow the default (they never chose Normal: it was simply the default); a Gentle
         * or Hard alarm keeps that as its own override.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `puzzleMix` TEXT")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `defaultDifficulty` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `settings` ADD COLUMN `isPlus` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `alarms` SET `difficulty` = -1 WHERE `difficulty` = 1")
            }
        }

        /** v6: a sound per alarm. Null (every existing alarm) means the phone's default alarm sound. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `alarms` ADD COLUMN `soundUri` TEXT")
            }
        }

        /** v5: how to wake up (puzzles / scan / both). An alarm that finished with a scan becomes "both". */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `alarms` ADD COLUMN `wakeMethod` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `alarms` SET `wakeMethod` = 2 WHERE `finishWithScan` = 1")
            }
        }

        /** v4: per-alarm difficulty preset. Existing alarms are Normal, which is what they were. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `alarms` ADD COLUMN `difficulty` INTEGER NOT NULL DEFAULT 1")
            }
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
