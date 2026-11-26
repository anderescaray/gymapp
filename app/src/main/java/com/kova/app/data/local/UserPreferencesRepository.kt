package com.kova.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kova.app.domain.model.AccentPalette
import com.kova.app.domain.model.ThemeMode
import com.kova.app.domain.model.UserPreferences
import com.kova.app.domain.model.WeightUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.kovaDataStore: DataStore<Preferences> by preferencesDataStore(name = "kova_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val TRACKING_START_EPOCH_DAY = longPreferencesKey("tracking_start_epoch_day")
        val WEEKLY_GOAL_DAYS = intPreferencesKey("weekly_goal_days")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val DEFAULT_REST_SECONDS = intPreferencesKey("default_rest_seconds")
        val AUTO_START_REST_TIMER = booleanPreferencesKey("auto_start_rest_timer")
        val SOUND_HAPTICS_ENABLED = booleanPreferencesKey("sound_haptics_enabled")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val GOOGLE_DRIVE_EMAIL = stringPreferencesKey("google_drive_email")
    }

    val preferencesFlow: Flow<UserPreferences> = context.kovaDataStore.data.map { prefs ->
        UserPreferences(
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
            trackingStartEpochDay = prefs[Keys.TRACKING_START_EPOCH_DAY] ?: LocalDate.now().toEpochDay(),
            weeklyGoalDays = (prefs[Keys.WEEKLY_GOAL_DAYS] ?: 4).coerceIn(1, 7),
            weightUnit = runCatching {
                WeightUnit.valueOf(prefs[Keys.WEIGHT_UNIT] ?: WeightUnit.KG.name)
            }.getOrDefault(WeightUnit.KG),
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            accentPalette = runCatching {
                AccentPalette.valueOf(prefs[Keys.ACCENT_PALETTE] ?: AccentPalette.VOLT.name)
            }.getOrDefault(AccentPalette.VOLT),
            defaultRestSeconds = (prefs[Keys.DEFAULT_REST_SECONDS] ?: 90).coerceIn(15, 600),
            autoStartRestTimer = prefs[Keys.AUTO_START_REST_TIMER] ?: true,
            soundAndHapticsEnabled = prefs[Keys.SOUND_HAPTICS_ENABLED] ?: true,
            lastBackupTimestampMillis = prefs[Keys.LAST_BACKUP_TIMESTAMP],
            googleDriveAccountEmail = prefs[Keys.GOOGLE_DRIVE_EMAIL]
        )
    }

    suspend fun getCurrentPreferences(): UserPreferences = preferencesFlow.first()

    suspend fun setOnboardingCompleted(completed: Boolean, trackingStartEpochDay: Long? = null) {
        context.kovaDataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_ONBOARDING] = completed
            if (trackingStartEpochDay != null) {
                prefs[Keys.TRACKING_START_EPOCH_DAY] = trackingStartEpochDay
            } else if (Keys.TRACKING_START_EPOCH_DAY !in prefs) {
                prefs[Keys.TRACKING_START_EPOCH_DAY] = LocalDate.now().toEpochDay()
            }
        }
    }

    suspend fun setTrackingStartEpochDay(epochDay: Long) {
        context.kovaDataStore.edit { it[Keys.TRACKING_START_EPOCH_DAY] = epochDay }
    }

    suspend fun setWeeklyGoalDays(days: Int) {
        context.kovaDataStore.edit { it[Keys.WEEKLY_GOAL_DAYS] = days.coerceIn(1, 7) }
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.kovaDataStore.edit { it[Keys.WEIGHT_UNIT] = unit.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.kovaDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccentPalette(palette: AccentPalette) {
        context.kovaDataStore.edit { it[Keys.ACCENT_PALETTE] = palette.name }
    }

    suspend fun setDefaultRestSeconds(seconds: Int) {
        context.kovaDataStore.edit { it[Keys.DEFAULT_REST_SECONDS] = seconds.coerceIn(15, 600) }
    }

    suspend fun setAutoStartRestTimer(enabled: Boolean) {
        context.kovaDataStore.edit { it[Keys.AUTO_START_REST_TIMER] = enabled }
    }

    suspend fun setSoundAndHapticsEnabled(enabled: Boolean) {
        context.kovaDataStore.edit { it[Keys.SOUND_HAPTICS_ENABLED] = enabled }
    }

    suspend fun setLastBackupTimestamp(timestampMillis: Long) {
        context.kovaDataStore.edit { it[Keys.LAST_BACKUP_TIMESTAMP] = timestampMillis }
    }

    suspend fun restorePreferences(prefs: UserPreferences) {
        context.kovaDataStore.edit { store ->
            store[Keys.HAS_COMPLETED_ONBOARDING] = prefs.hasCompletedOnboarding
            store[Keys.TRACKING_START_EPOCH_DAY] = prefs.trackingStartEpochDay
            store[Keys.WEEKLY_GOAL_DAYS] = prefs.weeklyGoalDays.coerceIn(1, 7)
            store[Keys.WEIGHT_UNIT] = prefs.weightUnit.name
            store[Keys.THEME_MODE] = prefs.themeMode.name
            store[Keys.ACCENT_PALETTE] = prefs.accentPalette.name
            store[Keys.DEFAULT_REST_SECONDS] = prefs.defaultRestSeconds.coerceIn(15, 600)
            store[Keys.AUTO_START_REST_TIMER] = prefs.autoStartRestTimer
            store[Keys.SOUND_HAPTICS_ENABLED] = prefs.soundAndHapticsEnabled
        }
    }
}
