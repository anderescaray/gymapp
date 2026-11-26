package com.kova.app.data.repository

import androidx.room.withTransaction
import com.kova.app.data.local.ExerciseEntity
import com.kova.app.data.local.KovaDatabase
import com.kova.app.data.local.RoutineDayEntity
import com.kova.app.data.local.UserPreferencesRepository
import com.kova.app.data.local.WorkoutSessionEntity
import com.kova.app.data.local.WorkoutSetEntity
import com.kova.app.data.local.toDomain
import com.kova.app.data.local.toEntity
import com.kova.app.domain.logic.DayGoalEvaluator
import com.kova.app.domain.logic.ProgressAnalyticsEngine
import com.kova.app.domain.model.AccentPalette
import com.kova.app.domain.model.Exercise
import com.kova.app.domain.model.RoutineDay
import com.kova.app.domain.model.RoutineWithExercises
import com.kova.app.domain.model.SessionWithSets
import com.kova.app.domain.model.ThemeMode
import com.kova.app.domain.model.UserPreferences
import com.kova.app.domain.model.WeightUnit
import com.kova.app.domain.model.WorkoutSession
import com.kova.app.domain.model.WorkoutSet
import com.kova.app.domain.sync.KovaBackupCodec
import com.kova.app.domain.sync.KovaBackupSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class FinishedWorkoutSummary(
    val sessionId: Long,
    val routineName: String,
    val routineColorHex: String,
    val durationSeconds: Long,
    val completedExercises: Int,
    val targetExercises: Int,
    val isDayGoalMet: Boolean,
    val totalSets: Int,
    val totalVolumeKg: Double,
    val newPrExerciseNames: List<String>
)

class KovaRepository(
    private val database: KovaDatabase,
    private val preferencesRepo: UserPreferencesRepository
) {
    private val routineDao = database.routineDayDao()
    private val exerciseDao = database.exerciseDao()
    private val sessionDao = database.workoutSessionDao()
    private val setDao = database.workoutSetDao()

    val preferencesFlow: Flow<UserPreferences> = preferencesRepo.preferencesFlow

    val routinesWithExercisesFlow: Flow<List<RoutineWithExercises>> = combine(
        routineDao.observeActiveRoutines(),
        exerciseDao.observeAllActiveExercises()
    ) { routines, exercises ->
        val byRoutine = exercises.groupBy { it.routineDayId }
        routines.map { routine ->
            RoutineWithExercises(
                routine = routine.toDomain(),
                exercises = byRoutine[routine.id].orEmpty().map { it.toDomain() }
            )
        }
    }

    val allSessionsFlow: Flow<List<WorkoutSession>> = sessionDao.observeAllSessions()
        .map { list -> list.map { it.toDomain() } }

    val allSetsFlow: Flow<List<WorkoutSet>> = setDao.observeAllSets()
        .map { list -> list.map { it.toDomain() } }

    val activeSessionWithSetsFlow: Flow<SessionWithSets?> = combine(
        sessionDao.observeActiveSession(),
        setDao.observeAllSets()
    ) { active, sets ->
        active?.let { sessionEntity ->
            SessionWithSets(
                session = sessionEntity.toDomain(),
                sets = sets.filter { it.sessionId == sessionEntity.id }.map { it.toDomain() }
            )
        }
    }

    // --- Preferences ---

    suspend fun completeOnboarding(weeklyGoalDays: Int) {
        preferencesRepo.setWeeklyGoalDays(weeklyGoalDays)
        preferencesRepo.setOnboardingCompleted(
            completed = true,
            trackingStartEpochDay = null
        )
    }

    suspend fun resetOnboardingFlag() {
        preferencesRepo.setOnboardingCompleted(completed = false)
    }

    suspend fun updateWeeklyGoal(days: Int) = preferencesRepo.setWeeklyGoalDays(days)
    suspend fun updateWeightUnit(unit: WeightUnit) = preferencesRepo.setWeightUnit(unit)
    suspend fun updateThemeMode(mode: ThemeMode) = preferencesRepo.setThemeMode(mode)
    suspend fun updateAccentPalette(palette: AccentPalette) = preferencesRepo.setAccentPalette(palette)
    suspend fun updateDefaultRestSeconds(seconds: Int) = preferencesRepo.setDefaultRestSeconds(seconds)
    suspend fun updateAutoStartRestTimer(enabled: Boolean) = preferencesRepo.setAutoStartRestTimer(enabled)
    suspend fun updateSoundAndHaptics(enabled: Boolean) = preferencesRepo.setSoundAndHapticsEnabled(enabled)

    // --- Routines & Exercises ---

    suspend fun createRoutineDay(
        name: String,
        colorHex: String,
        targetExerciseCount: Int
    ): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        val existing = routineDao.getAllRoutinesOnce().filterNot { it.isArchived }
        return routineDao.insert(
            RoutineDayEntity(
                name = trimmed,
                colorHex = colorHex,
                targetExerciseCount = targetExerciseCount.coerceAtLeast(1),
                sortOrder = existing.size,
                isArchived = false,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateRoutineDay(routine: RoutineDay) {
        routineDao.update(routine.copy(name = routine.name.trim()).toEntity())
    }

    suspend fun archiveRoutineDay(routineId: Long) {
        routineDao.archive(routineId)
    }

    suspend fun createExercise(
        routineDayId: Long,
        name: String,
        isMandatory: Boolean,
        isBodyweight: Boolean = false,
        notes: String = "",
        defaultRestSeconds: Int? = null
    ): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        val existing = exerciseDao.getExercisesForRoutineOnce(routineDayId)
        val newId = exerciseDao.insert(
            ExerciseEntity(
                routineDayId = routineDayId,
                name = trimmed,
                isMandatory = isMandatory,
                isBodyweight = isBodyweight,
                notes = notes.trim(),
                defaultRestSeconds = defaultRestSeconds,
                sortOrder = existing.size,
                isArchived = false,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
        // Ensure routine target is at least equal to mandatory count
        val routine = routineDao.getById(routineDayId)
        if (routine != null && isMandatory) {
            val mandatoryCount = existing.count { it.isMandatory } + 1
            if (routine.targetExerciseCount < mandatoryCount) {
                routineDao.update(routine.copy(targetExerciseCount = mandatoryCount))
            }
        }
        return newId
    }

    suspend fun updateExercise(exercise: Exercise) {
        exerciseDao.update(exercise.copy(name = exercise.name.trim(), notes = exercise.notes.trim()).toEntity())
        val routine = routineDao.getById(exercise.routineDayId)
        if (routine != null) {
            val mandatoryCount = exerciseDao.getExercisesForRoutineOnce(exercise.routineDayId).count { it.isMandatory }
            if (routine.targetExerciseCount < mandatoryCount) {
                routineDao.update(routine.copy(targetExerciseCount = mandatoryCount))
            }
        }
    }

    suspend fun updateExerciseManualOneRm(
        exerciseId: Long,
        oneRmKg: Double?,
        dateEpochDay: Long? = LocalDate.now().toEpochDay()
    ) {
        val sanitized = oneRmKg?.takeIf { it.isFinite() && it > 0.0 }?.coerceAtMost(2000.0)
        exerciseDao.updateManualOneRm(
            exerciseId = exerciseId,
            oneRmKg = sanitized,
            dateEpochDay = if (sanitized != null) (dateEpochDay ?: LocalDate.now().toEpochDay()) else null
        )
    }

    suspend fun archiveExercise(exerciseId: Long) {
        exerciseDao.archive(exerciseId)
    }

    // --- Active Workout Session ---

    suspend fun startOrResumeWorkout(routineDayId: Long): Long {
        val existingActive = sessionDao.getActiveSessionOnce()
        if (existingActive != null) {
            return existingActive.id
        }
        val routine = routineDao.getById(routineDayId)
            ?: throw IllegalArgumentException("Rutina no encontrada ($routineDayId)")
        val exercises = exerciseDao.getExercisesForRoutineOnce(routineDayId)
        val mandatoryCount = exercises.count { it.isMandatory }
        val effectiveTarget = maxOf(routine.targetExerciseCount, mandatoryCount)

        return sessionDao.insert(
            WorkoutSessionEntity(
                routineDayId = routine.id,
                routineDayNameSnapshot = routine.name,
                routineColorHexSnapshot = routine.colorHex,
                dateEpochDay = LocalDate.now().toEpochDay(),
                startedAtEpochMillis = System.currentTimeMillis(),
                finishedAtEpochMillis = null,
                targetExerciseCountSnapshot = effectiveTarget,
                mandatoryCountSnapshot = mandatoryCount,
                completedExerciseCount = 0,
                allMandatoryCompleted = false,
                isDayGoalMet = false,
                sessionNote = ""
            )
        )
    }

    suspend fun addSetToSession(
        sessionId: Long,
        exercise: Exercise,
        weightKg: Double,
        reps: Int,
        isCompleted: Boolean = false
    ): Long {
        val existingForExercise = setDao.getSetsForSessionOnce(sessionId)
            .filter { it.exerciseId == exercise.id }
        val nextIndex = (existingForExercise.maxOfOrNull { it.setIndex } ?: 0) + 1
        return setDao.insert(
            WorkoutSetEntity(
                sessionId = sessionId,
                exerciseId = exercise.id,
                exerciseNameSnapshot = exercise.name,
                isMandatorySnapshot = exercise.isMandatory,
                isBodyweightSnapshot = exercise.isBodyweight,
                setIndex = nextIndex,
                weightKg = weightKg.coerceAtLeast(0.0),
                reps = reps.coerceAtLeast(0),
                isCompleted = isCompleted,
                completedAtEpochMillis = if (isCompleted) System.currentTimeMillis() else null
            )
        )
    }

    suspend fun copyPreviousSetsToSession(
        sessionId: Long,
        exercise: Exercise,
        previousSets: List<WorkoutSet>
    ) {
        if (previousSets.isEmpty()) return
        val existing = setDao.getSetsForSessionOnce(sessionId)
            .filter { it.exerciseId == exercise.id }
        var nextIndex = (existing.maxOfOrNull { it.setIndex } ?: 0) + 1
        val newEntities = previousSets.sortedBy { it.setIndex }.map { prev ->
            WorkoutSetEntity(
                sessionId = sessionId,
                exerciseId = exercise.id,
                exerciseNameSnapshot = exercise.name,
                isMandatorySnapshot = exercise.isMandatory,
                isBodyweightSnapshot = exercise.isBodyweight,
                setIndex = nextIndex++,
                weightKg = prev.weightKg,
                reps = prev.reps,
                isCompleted = false,
                completedAtEpochMillis = null
            )
        }
        setDao.insertAll(newEntities)
    }

    suspend fun updateWorkoutSet(set: WorkoutSet) {
        val completedMillis = if (set.isCompleted) {
            set.completedAtEpochMillis ?: System.currentTimeMillis()
        } else null
        setDao.update(
            set.copy(
                weightKg = set.weightKg.coerceAtLeast(0.0),
                reps = set.reps.coerceAtLeast(0),
                completedAtEpochMillis = completedMillis
            ).toEntity()
        )
    }

    suspend fun deleteWorkoutSet(setId: Long, sessionId: Long, exerciseId: Long?) {
        setDao.deleteById(setId)
        if (exerciseId != null) {
            val remaining = setDao.getSetsForSessionOnce(sessionId)
                .filter { it.exerciseId == exerciseId }
                .sortedBy { it.setIndex }
            remaining.forEachIndexed { idx, entity ->
                if (entity.setIndex != idx + 1) {
                    setDao.update(entity.copy(setIndex = idx + 1))
                }
            }
        }
    }

    suspend fun finishActiveSession(
        sessionId: Long,
        sessionNote: String = ""
    ): FinishedWorkoutSummary? = database.withTransaction {
        val sessionEntity = sessionDao.getById(sessionId) ?: return@withTransaction null
        // Drop uncompleted draft rows so history and analytics stay spotless
        setDao.deleteIncompleteForSession(sessionId)
        val completedSets = setDao.getSetsForSessionOnce(sessionId).map { it.toDomain() }
        if (completedSets.isEmpty()) {
            sessionDao.deleteById(sessionId)
            return@withTransaction null
        }

        val routineExercises = sessionEntity.routineDayId?.let {
            exerciseDao.getExercisesForRoutineOnce(it).map { e -> e.toDomain() }
        }.orEmpty()

        val evaluation = DayGoalEvaluator.evaluate(
            routineExercises = routineExercises,
            targetExerciseCount = sessionEntity.targetExerciseCountSnapshot,
            sessionSets = completedSets
        )

        val now = System.currentTimeMillis()
        val updated = sessionEntity.copy(
            finishedAtEpochMillis = now,
            targetExerciseCountSnapshot = evaluation.targetCount,
            mandatoryCountSnapshot = evaluation.mandatoryTotal,
            completedExerciseCount = evaluation.totalCompleted,
            allMandatoryCompleted = evaluation.mandatoryRemaining == 0,
            isDayGoalMet = evaluation.isComplete,
            sessionNote = sessionNote.trim()
        )
        sessionDao.update(updated)

        // Identify PRs achieved during this session
        val allPriorSets = setDao.getAllSetsOnce()
            .filter { it.sessionId != sessionId && it.isCompleted && it.reps > 0 }
            .map { it.toDomain() }
            .groupBy { it.exerciseId }

        val prNames = mutableListOf<String>()
        completedSets.groupBy { it.exerciseId }.forEach { (exId, exSets) ->
            if (exId == null) return@forEach
            val prior = allPriorSets[exId].orEmpty()
            if (prior.isNotEmpty()) {
                val achievedPr = exSets.any { s ->
                    ProgressAnalyticsEngine.isSetNewPr(
                        weightKg = s.weightKg,
                        reps = s.reps,
                        isBodyweight = s.isBodyweightSnapshot,
                        historicalSets = prior
                    )
                }
                if (achievedPr) {
                    prNames += exSets.first().exerciseNameSnapshot
                }
            }
        }

        val durationSec = ((now - sessionEntity.startedAtEpochMillis) / 1000L).coerceAtLeast(1L)
        FinishedWorkoutSummary(
            sessionId = sessionId,
            routineName = sessionEntity.routineDayNameSnapshot,
            routineColorHex = sessionEntity.routineColorHexSnapshot,
            durationSeconds = durationSec,
            completedExercises = evaluation.totalCompleted,
            targetExercises = evaluation.targetCount,
            isDayGoalMet = evaluation.isComplete,
            totalSets = completedSets.size,
            totalVolumeKg = completedSets.sumOf { it.weightKg * it.reps },
            newPrExerciseNames = prNames.distinct()
        )
    }

    suspend fun discardActiveSession(sessionId: Long) {
        sessionDao.deleteById(sessionId)
    }

    suspend fun deleteFinishedSession(sessionId: Long) {
        sessionDao.deleteById(sessionId)
    }

    // --- Backup & Demo Data ---

    suspend fun buildBackupSnapshot(): KovaBackupSnapshot {
        val prefs = preferencesRepo.getCurrentPreferences()
        val routines = routineDao.getAllRoutinesOnce().map { it.toDomain() }
        val exercises = exerciseDao.getAllExercisesOnce().map { it.toDomain() }
        val sessions = sessionDao.getAllSessionsOnce().map { it.toDomain() }
        val sets = setDao.getAllSetsOnce().map { it.toDomain() }
        return KovaBackupSnapshot(
            exportedAtEpochMillis = System.currentTimeMillis(),
            preferences = prefs,
            routineDays = routines,
            exercises = exercises,
            workoutSessions = sessions,
            workoutSets = sets
        )
    }

    suspend fun exportBackupJson(): String {
        val snapshot = buildBackupSnapshot()
        val encoded = KovaBackupCodec.encode(snapshot)
        preferencesRepo.setLastBackupTimestamp(snapshot.exportedAtEpochMillis)
        return encoded
    }

    suspend fun importBackupJson(json: String): Result<Unit> {
        val decoded = KovaBackupCodec.decode(json)
        val snapshot = decoded.getOrElse { return Result.failure(it) }
        return runCatching {
            database.withTransaction {
                setDao.deleteAll()
                sessionDao.deleteAll()
                exerciseDao.deleteAll()
                routineDao.deleteAll()

                routineDao.insertAll(snapshot.routineDays.map { it.toEntity() })
                exerciseDao.insertAll(snapshot.exercises.map { it.toEntity() })
                sessionDao.insertAll(snapshot.workoutSessions.map { it.toEntity() })
                setDao.insertAll(snapshot.workoutSets.map { it.toEntity() })
            }
            preferencesRepo.restorePreferences(snapshot.preferences)
        }
    }

    suspend fun clearAllWorkoutAndRoutineData() {
        database.withTransaction {
            setDao.deleteAll()
            sessionDao.deleteAll()
            exerciseDao.deleteAll()
            routineDao.deleteAll()
        }
        preferencesRepo.setTrackingStartEpochDay(LocalDate.now().toEpochDay())
    }
}
