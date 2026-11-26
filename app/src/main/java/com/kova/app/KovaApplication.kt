package com.kova.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.kova.app.data.local.KovaDatabase
import com.kova.app.data.local.UserPreferencesRepository
import com.kova.app.data.repository.KovaRepository
import com.kova.app.domain.sync.CloudSyncProvider
import com.kova.app.domain.sync.GoogleDriveSyncManager
import com.kova.app.ui.state.RestTimerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KovaApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isAppInForeground = MutableStateFlow(true)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    val database: KovaDatabase by lazy { KovaDatabase.getInstance(this) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val repository: KovaRepository by lazy { KovaRepository(database, preferencesRepository) }
    val restTimerManager: RestTimerManager by lazy { RestTimerManager(this, appScope) }
    val cloudSyncProvider: CloudSyncProvider by lazy { GoogleDriveSyncManager() }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var startedActivities = 0
            private var isChangingConfigurations = false

            override fun onActivityStarted(activity: Activity) {
                val wasChanging = isChangingConfigurations
                isChangingConfigurations = false
                if (++startedActivities == 1 && !wasChanging) {
                    _isAppInForeground.value = true
                }
            }

            override fun onActivityStopped(activity: Activity) {
                isChangingConfigurations = activity.isChangingConfigurations
                startedActivities = (startedActivities - 1).coerceAtLeast(0)
                if (startedActivities == 0 && !isChangingConfigurations) {
                    _isAppInForeground.value = false
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
