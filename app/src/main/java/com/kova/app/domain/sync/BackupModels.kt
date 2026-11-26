package com.kova.app.domain.sync

import com.kova.app.domain.model.Exercise
import com.kova.app.domain.model.RoutineDay
import com.kova.app.domain.model.UserPreferences
import com.kova.app.domain.model.WorkoutSession
import com.kova.app.domain.model.WorkoutSet

const val KOVA_BACKUP_SCHEMA_VERSION = 1

data class KovaBackupSnapshot(
    val schemaVersion: Int = KOVA_BACKUP_SCHEMA_VERSION,
    val exportedAtEpochMillis: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val preferences: UserPreferences,
    val routineDays: List<RoutineDay>,
    val exercises: List<Exercise>,
    val workoutSessions: List<WorkoutSession>,
    val workoutSets: List<WorkoutSet>
)
