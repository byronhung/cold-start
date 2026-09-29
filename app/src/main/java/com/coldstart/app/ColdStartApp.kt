package com.coldstart.app

import android.app.Application
import com.coldstart.app.data.AlarmRepository
import com.coldstart.app.data.ColdStartDatabase

/**
 * Holds the app's single database and repository. Grit used Hilt for this; one repository
 * doesn't need a dependency-injection framework.
 */
class ColdStartApp : Application() {
    val repository: AlarmRepository by lazy {
        AlarmRepository(ColdStartDatabase.build(this).alarmDao())
    }
}
