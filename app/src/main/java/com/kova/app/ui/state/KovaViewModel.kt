package com.kova.app.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kova.app.data.repository.FinishedWorkoutSummary
import com.kova.app.data.repository.KovaRepository
import com.kova.app.domain.logic.CalendarEvaluator
import com.kova.app.domain.logic.ProgressAnalyticsEngine
import com.kova.app.domain.logic.RoutineSuggester
import com.kova.app.domain.model.AccentPalette
import com.kova.app.domain.model.CalendarWeekSummary
import com.kova.app.domain.model.Exercise
import com.kova.app.domain.model.ExerciseAnalytics
import com.kova.app.domain.model.RoutineDay
import com.kova.app.domain.model.RoutineWithExercises
import com.kova.app.domain.model.SessionWithSets
import com.kova.app.domain.model.ThemeMode
import com.kova.app.domain.model.UserPreferences
import com.kova.app.domain.model.WeightUnit
import com.kova.app.domain.model.WorkoutSession
import com.kova.app.domain.model.WorkoutSet
import com.kova.app.domain.sync.CloudSyncProvider
import com.kova.app.domain.sync.CloudSyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class KovaUiState(
    val isLoading: Boolean = true,
    val preferences: UserPreferences = UserPreferences(),
    val routines: List<RoutineWithExercises> = emptyList(),
    val allSessions: List<WorkoutSession> = emptyList(),
    val allSets: List<WorkoutSet> = emptyList(),
    val activeSession: SessionWithSets? = null,
    val suggestedRoutine: RoutineDay? = null,
    val currentWeekSummary: CalendarWeekSummary? = null,
    val weeklyStreak: Int = 0,
    val thisMonthWorkoutCount: Int = 0,
    val cloudSyncStatus: CloudSyncStatus = CloudSyncStatus.ReadyToConfigure
)

class KovaViewModel(
    private val repository: KovaRepository,
    val restTimerManager: RestTimerManager,
    private val cloudSyncProvider: CloudSyncProvider
) : ViewModel() {

    val restTimerState: StateFlow<RestTimerState> = restTimerManager.state

    private val _finishedSummary = MutableStateFlow<FinishedWorkoutSummary?>(null)
    val finishedSummary: StateFlow<FinishedWorkoutSummary?> = _finishedSummary.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _livePrExerciseIds = MutableStateFlow<Set<Long>>(emptySet())
    val livePrExerciseIds: StateFlow<Set<Long>> = _livePrExerciseIds.asStateFlow()

    init {
        viewModelScope.launch {
            repository.preferencesFlow.collect { prefs ->
                restTimerManager.setSoundAndHapticsEnabled(prefs.soundAndHapticsEnabled)
                restTimerManager.setAccentColor(prefs.accentPalette.previewHexDark.toInt())
            }
        }
    }

    val uiState: StateFlow<KovaUiState> = combine(
        repository.preferencesFlow,
        repository.routinesWithExercisesFlow,
        repository.allSessionsFlow,
        repository.allSetsFlow,
        repository.activeSessionWithSetsFlow,
        cloudSyncProvider.status
    ) { values ->
        val prefs = values[0] as UserPreferences
        @Suppress("UNCHECKED_CAST")
        val routines = values[1] as List<RoutineWithExercises>
        @Suppress("UNCHECKED_CAST")
        val sessions = values[2] as List<WorkoutSession>
        @Suppress("UNCHECKED_CAST")
        val sets = values[3] as List<WorkoutSet>
        val activeSession = values[4] as SessionWithSets?
        val syncStatus = values[5] as CloudSyncStatus

        val today = LocalDate.now()
        val trackingStart = LocalDate.ofEpochDay(prefs.trackingStartEpochDay)
        val finishedSessions = sessions.filter { !it.isActive }
        val suggested = RoutineSuggester.suggestNextRoutine(
            routines = routines.map { it.routine },
            finishedSessions = finishedSessions
        )
        val currentWeek = CalendarEvaluator.evaluateCurrentWeek(
            finishedSessions = finishedSessions,
            weeklyGoal = prefs.weeklyGoalDays,
            trackingStartDate = trackingStart,
            today = today
        )
        val streak = CalendarEvaluator.calculateWeeklyStreak(
            finishedSessions = finishedSessions,
            weeklyGoal = prefs.weeklyGoalDays,
            trackingStartDate = trackingStart,
            today = today
        )
        val monthCount = CalendarEvaluator.countWorkoutsInMonth(
            month = YearMonth.from(today),
            finishedSessions = finishedSessions
        )

        KovaUiState(
            isLoading = false,
            preferences = prefs,
            routines = routines,
            allSessions = sessions,
            allSets = sets,
            activeSession = activeSession,
            suggestedRoutine = suggested,
            currentWeekSummary = currentWeek,
            weeklyStreak = streak,
            thisMonthWorkoutCount = monthCount,
            cloudSyncStatus = syncStatus
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = KovaUiState(isLoading = true)
    )

    // --- Derived Analytics & Helpers ---

    fun getExerciseAnalytics(exerciseId: Long): ExerciseAnalytics {
        val state = uiState.value
        val exercise = state.routines
            .flatMap { it.exercises }
            .firstOrNull { it.id == exerciseId }
        val sessionsById = state.allSessions.associateBy { it.id }
        val setsBySession = state.allSets
            .filter { it.exerciseId == exerciseId && it.isCompleted && it.reps > 0 }
            .groupBy { it.sessionId }

        val pairs = setsBySession.mapNotNull { (sessionId, sets) ->
            val session = sessionsById[sessionId] ?: return@mapNotNull null
            if (session.isActive) return@mapNotNull null
            session to sets
        }

        val isBw = exercise?.isBodyweight
            ?: state.allSets.firstOrNull { it.exerciseId == exerciseId }?.isBodyweightSnapshot
            ?: false

        return ProgressAnalyticsEngine.analyzeExercise(
            sessionsWithSets = pairs,
            isBodyweightExercise = isBw,
            unit = state.preferences.weightUnit
        )
    }

    fun getLastSessionSetsForExercise(exerciseId: Long, excludeSessionId: Long? = null): List<WorkoutSet> {
        val state = uiState.value
        val sessionsById = state.allSessions.associateBy { it.id }
        val candidateSessions = state.allSets
            .filter {
                it.exerciseId == exerciseId &&
                    it.sessionId != excludeSessionId &&
                    it.isCompleted &&
                    it.reps > 0
            }
            .groupBy { it.sessionId }
            .mapNotNull { (sId, sets) ->
                val session = sessionsById[sId] ?: return@mapNotNull null
                if (session.isActive) return@mapNotNull null
                session to sets.sortedBy { it.setIndex }
            }
            .maxWithOrNull(
                compareBy<Pair<WorkoutSession, List<WorkoutSet>>> { it.first.dateEpochDay }
                    .thenBy { it.first.finishedAtEpochMillis ?: it.first.startedAtEpochMillis }
                    .thenBy { it.first.id }
            )
        return candidateSessions?.second.orEmpty()
    }

    fun getSetsForSession(sessionId: Long): List<WorkoutSet> {
        return uiState.value.allSets
            .filter { it.sessionId == sessionId }
            .sortedWith(compareBy<WorkoutSet> { it.exerciseId ?: Long.MAX_VALUE }.thenBy { it.setIndex })
    }

    // --- Onboarding & Preferences ---

    fun completeOnboarding(weeklyGoalDays: Int) {
        viewModelScope.launch {
            repository.completeOnboarding(weeklyGoalDays)
        }
    }

    fun reopenOnboarding() {
        viewModelScope.launch {
            repository.resetOnboardingFlag()
        }
    }

    fun updateWeeklyGoal(days: Int) = viewModelScope.launch { repository.updateWeeklyGoal(days) }
    fun updateWeightUnit(unit: WeightUnit) = viewModelScope.launch { repository.updateWeightUnit(unit) }
    fun updateThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.updateThemeMode(mode) }
    fun updateAccentPalette(palette: AccentPalette) = viewModelScope.launch { repository.updateAccentPalette(palette) }
    fun updateDefaultRestSeconds(seconds: Int) = viewModelScope.launch { repository.updateDefaultRestSeconds(seconds) }
    fun updateAutoStartRestTimer(enabled: Boolean) = viewModelScope.launch { repository.updateAutoStartRestTimer(enabled) }
    fun updateSoundAndHaptics(enabled: Boolean) = viewModelScope.launch { repository.updateSoundAndHaptics(enabled) }

    // --- Routines & Exercises ---

    fun createRoutineDay(
        name: String,
        colorHex: String,
        targetExerciseCount: Int,
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                repository.createRoutineDay(name, colorHex, targetExerciseCount)
            }.onSuccess { id ->
                onCreated(id)
            }
        }
    }

    fun updateRoutineDay(routine: RoutineDay) {
        viewModelScope.launch { repository.updateRoutineDay(routine) }
    }

    fun archiveRoutineDay(routineId: Long) {
        viewModelScope.launch {
            repository.archiveRoutineDay(routineId)
            showMessage("Día de rutina eliminado")
        }
    }

    fun createExercise(
        routineDayId: Long,
        name: String,
        isMandatory: Boolean,
        defaultRestSeconds: Int? = null,
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                repository.createExercise(
                    routineDayId = routineDayId,
                    name = name,
                    isMandatory = isMandatory,
                    isBodyweight = false,
                    notes = "",
                    defaultRestSeconds = defaultRestSeconds
                )
            }.onSuccess { id ->
                onCreated(id)
            }
        }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch { repository.updateExercise(exercise) }
    }

    fun updateExerciseManualOneRm(exerciseId: Long, oneRmKg: Double?) {
        viewModelScope.launch {
            repository.updateExerciseManualOneRm(exerciseId, oneRmKg)
            showMessage(if (oneRmKg != null) "1RM real guardada" else "1RM real eliminada")
        }
    }

    fun archiveExercise(exerciseId: Long) {
        viewModelScope.launch {
            repository.archiveExercise(exerciseId)
            showMessage("Ejercicio eliminado de la rutina")
        }
    }

    // --- Workout Session Flow ---

    fun startWorkout(routineDayId: Long, onStarted: (Long) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.startOrResumeWorkout(routineDayId) }
                .onSuccess { id -> onStarted(id) }
                .onFailure { showMessage("No se pudo iniciar la rutina") }
        }
    }

    fun addSet(sessionId: Long, exercise: Exercise, weightKg: Double, reps: Int) {
        viewModelScope.launch {
            repository.addSetToSession(
                sessionId = sessionId,
                exercise = exercise,
                weightKg = weightKg,
                reps = reps,
                isCompleted = false
            )
        }
    }

    fun copyLastSessionSets(sessionId: Long, exercise: Exercise) {
        val lastSets = getLastSessionSetsForExercise(exercise.id, excludeSessionId = sessionId)
        if (lastSets.isEmpty()) return
        viewModelScope.launch {
            repository.copyPreviousSetsToSession(sessionId, exercise, lastSets)
            showMessage("Copiadas ${lastSets.size} series de la última vez")
        }
    }

    fun updateSet(set: WorkoutSet, exercise: Exercise?, justCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateWorkoutSet(set)
            if (justCompleted && set.reps > 0) {
                val prefs = uiState.value.preferences
                if (prefs.autoStartRestTimer) {
                    val latestExercise = exercise?.let { ex ->
                        uiState.value.routines
                            .asSequence()
                            .flatMap { it.exercises.asSequence() }
                            .firstOrNull { it.id == ex.id }
                    } ?: exercise
                    val duration = latestExercise?.defaultRestSeconds ?: prefs.defaultRestSeconds
                    restTimerManager.start(duration, set.exerciseNameSnapshot)
                }
                // Check live PR against ALL prior completed sets (past sessions AND earlier sets in this session)
                if (set.exerciseId != null) {
                    val pastSessionsSets = uiState.value.allSets.filter {
                        it.exerciseId == set.exerciseId &&
                            it.sessionId != set.sessionId &&
                            it.isCompleted &&
                            it.reps > 0
                    }
                    val otherSetsIncludingToday = uiState.value.allSets.filter {
                        it.exerciseId == set.exerciseId &&
                            it.id != set.id &&
                            it.isCompleted &&
                            it.reps > 0
                    }
                    if (pastSessionsSets.isNotEmpty() &&
                        ProgressAnalyticsEngine.isSetNewPr(set.weightKg, set.reps, set.isBodyweightSnapshot, otherSetsIncludingToday)
                    ) {
                        _livePrExerciseIds.value = _livePrExerciseIds.value + set.exerciseId
                        showMessage("¡Nuevo récord personal en ${set.exerciseNameSnapshot}!")
                    }
                }
            }
        }
    }

    fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch {
            repository.deleteWorkoutSet(set.id, set.sessionId, set.exerciseId)
        }
    }

    fun finishActiveSession(sessionNote: String = "", onFinished: () -> Unit = {}) {
        val activeId = uiState.value.activeSession?.session?.id ?: return
        viewModelScope.launch {
            restTimerManager.dismiss()
            val summary = repository.finishActiveSession(activeId, sessionNote)
            _livePrExerciseIds.value = emptySet()
            _finishedSummary.value = summary
            onFinished()
        }
    }

    fun dismissFinishedSummary() {
        _finishedSummary.value = null
    }

    fun discardActiveSession(onDiscarded: () -> Unit = {}) {
        val activeId = uiState.value.activeSession?.session?.id ?: return
        viewModelScope.launch {
            restTimerManager.dismiss()
            repository.discardActiveSession(activeId)
            _livePrExerciseIds.value = emptySet()
            showMessage("Entrenamiento descartado")
            onDiscarded()
        }
    }

    fun deleteFinishedSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteFinishedSession(sessionId)
            showMessage("Sesión eliminada del historial")
        }
    }

    // --- Backup, Cloud & Demo Data ---

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    fun importBackupJson(json: String) {
        viewModelScope.launch {
            val result = repository.importBackupJson(json)
            if (result.isSuccess) {
                restTimerManager.dismiss()
                _livePrExerciseIds.value = emptySet()
                showMessage("Copia de seguridad restaurada correctamente")
            } else {
                showMessage("No se pudo importar: ${result.exceptionOrNull()?.message ?: "archivo inválido"}")
            }
        }
    }

    fun testGoogleDrivePreparation() {
        viewModelScope.launch {
            val snapshot = repository.buildBackupSnapshot()
            val res = cloudSyncProvider.uploadSnapshot(snapshot)
            val msg = res.exceptionOrNull()?.message
                ?: "Copia sincronizada con Google Drive"
            showMessage(msg)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            restTimerManager.dismiss()
            repository.clearAllWorkoutAndRoutineData()
            showMessage("Todos los entrenamientos y rutinas borrados")
        }
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun consumeMessage() {
        _snackbarMessage.value = null
    }

    companion object {
        fun factory(
            repository: KovaRepository,
            restTimerManager: RestTimerManager,
            cloudSyncProvider: CloudSyncProvider
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return KovaViewModel(repository, restTimerManager, cloudSyncProvider) as T
            }
        }
    }
}
