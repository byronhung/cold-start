package com.coldstart.app

import android.app.Application
import com.coldstart.app.alarm.AlarmScheduler
import com.coldstart.app.alarm.PendingCheckStore
import com.coldstart.app.data.AlarmRepository
import com.coldstart.app.data.ColdStartDatabase
import com.coldstart.app.ring.Notifications
import com.coldstart.app.ring.RingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Holds the app's single database, repository and ring controller. Grit used Hilt for this; a
 * handful of objects doesn't need a dependency-injection framework.
 */
class ColdStartApp : Application() {
    /** Outlives any screen: for work that must finish even if the screen closes (saving a wake). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val scheduler: AlarmScheduler by lazy { AlarmScheduler(this) }
    val repository: AlarmRepository by lazy { AlarmRepository(ColdStartDatabase.build(this), scheduler, PendingCheckStore(this)) }
    val ringController: RingController by lazy { RingController(repository, appScope) }

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
        // Covers the cases no broadcast announces, like a force-stop wiping every scheduled alarm.
        appScope.launch { repository.rescheduleAll() }
    }
}
