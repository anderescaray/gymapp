package com.kova.app.domain.model

import java.time.LocalDate

enum class WeightUnit(val label: String, val stepLabel: String) {
    KG("kg", "2.5 kg"),
    LB("lb", "5 lb")
}

enum class ThemeMode(val displayName: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Oscuro")
}

enum class AccentPalette(
    val displayName: String,
    val description: String,
    val previewHexLight: Long,
    val previewHexDark: Long
) {
    VOLT("Volt", "Lima atlético", 0xFF3D7A00, 0xFFC6F635),
    CORAL("Coral", "Bermellón vivo", 0xFFD94426, 0xFFFF6B4A),
    COBALT("Cobalto", "Azul precisión", 0xFF0A66FF, 0xFF5C9DFF),
    EMERALD("Esmeralda", "Verde nórdico", 0xFF0E8A5F, 0xFF34D399),
    AMBER("Ámbar", "Oro titanio", 0xFFC26D00, 0xFFFBBF24),
    ROSE("Rosa", "Rosita claro", 0xFFF6A6C1, 0xFFF7A8C4)
}

object RoutineColorPresets {
    val hexPalette: List<String> = listOf(
        "#C6F635", // Volt
        "#FF6B4A", // Coral
        "#4D94FF", // Cobalt
        "#34D399", // Emerald
        "#FBBF24", // Amber
        "#A78BFA", // Soft Iris
        "#F472B6", // Rose
        "#38BDF8"  // Sky
    )
}

data class UserPreferences(
    val hasCompletedOnboarding: Boolean = false,
    val trackingStartEpochDay: Long = LocalDate.now().toEpochDay(),
    val weeklyGoalDays: Int = 4,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentPalette: AccentPalette = AccentPalette.VOLT,
    val defaultRestSeconds: Int = 90,
    val autoStartRestTimer: Boolean = true,
    val soundAndHapticsEnabled: Boolean = true,
    val lastBackupTimestampMillis: Long? = null,
    val googleDriveAccountEmail: String? = null
)

data class RoutineDay(
    val id: Long = 0L,
    val name: String,
    val colorHex: String = RoutineColorPresets.hexPalette.first(),
    val targetExerciseCount: Int = 5,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

data class Exercise(
    val id: Long = 0L,
    val routineDayId: Long,
    val name: String,
    val isMandatory: Boolean = true,
    val isBodyweight: Boolean = false,
    val notes: String = "",
    val defaultRestSeconds: Int? = null,
    val manualOneRmKg: Double? = null,
    val manualOneRmDateEpochDay: Long? = null,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

data class RoutineWithExercises(
    val routine: RoutineDay,
    val exercises: List<Exercise>
) {
    val activeExercises: List<Exercise>
        get() = exercises.filterNot { it.isArchived }
            .sortedWith(compareByDescending<Exercise> { it.isMandatory }.thenBy { it.sortOrder }.thenBy { it.id })

    val mandatoryCount: Int
        get() = activeExercises.count { it.isMandatory }

    val optionalCount: Int
        get() = activeExercises.count { !it.isMandatory }

    val effectiveTarget: Int
        get() = maxOf(routine.targetExerciseCount, mandatoryCount).coerceAtMost(activeExercises.size.coerceAtLeast(routine.targetExerciseCount))
}

data class WorkoutSession(
    val id: Long = 0L,
    val routineDayId: Long?,
    val routineDayNameSnapshot: String,
    val routineColorHexSnapshot: String,
    val dateEpochDay: Long,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long? = null,
    val targetExerciseCountSnapshot: Int,
    val mandatoryCountSnapshot: Int,
    val completedExerciseCount: Int = 0,
    val allMandatoryCompleted: Boolean = false,
    val isDayGoalMet: Boolean = false,
    val sessionNote: String = ""
) {
    val isActive: Boolean get() = finishedAtEpochMillis == null
    val date: LocalDate get() = LocalDate.ofEpochDay(dateEpochDay)
}

data class WorkoutSet(
    val id: Long = 0L,
    val sessionId: Long,
    val exerciseId: Long?,
    val exerciseNameSnapshot: String,
    val isMandatorySnapshot: Boolean,
    val isBodyweightSnapshot: Boolean,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val isCompleted: Boolean = false,
    val completedAtEpochMillis: Long? = null
)

data class SessionWithSets(
    val session: WorkoutSession,
    val sets: List<WorkoutSet>
) {
    val completedSets: List<WorkoutSet>
        get() = sets.filter { it.isCompleted && it.reps > 0 }

    val totalVolumeKg: Double
        get() = completedSets.sumOf { it.weightKg * it.reps }
}

enum class ChartMetric(val label: String, val shortLabel: String) {
    ESTIMATED_1RM("1RM est.", "1RM"),
    MAX_WEIGHT("Peso máx.", "Peso"),
    TOTAL_VOLUME("Volumen", "Vol."),
    BEST_REPS("Reps", "Reps")
}

enum class ProgressTrend(val badgeLabel: String) {
    NEW_PR("Nuevo récord · PR"),
    IMPROVING("En progreso"),
    PLATEAU("Estancado"),
    DIP("Ligera bajada"),
    BUILDING_HISTORY("Empezando")
}

data class ExerciseSessionPoint(
    val sessionId: Long,
    val date: LocalDate,
    val sets: List<WorkoutSet>,
    val estimated1RmKg: Double,
    val maxWeightKg: Double,
    val totalVolumeKg: Double,
    val bestReps: Int,
    val totalReps: Int,
    val isPureBodyweight: Boolean,
    val bestSetDescription: String,
    val progressionScore: Double
) {
    fun valueFor(metric: ChartMetric, isPureBodyweightSeries: Boolean = isPureBodyweight): Double = when (metric) {
        ChartMetric.ESTIMATED_1RM -> when {
            isPureBodyweightSeries -> bestReps.toDouble()
            isPureBodyweight -> progressionScore
            else -> if (estimated1RmKg > 0.0 && sets.none { it.isBodyweightSnapshot }) estimated1RmKg else progressionScore
        }
        ChartMetric.MAX_WEIGHT -> maxWeightKg
        ChartMetric.TOTAL_VOLUME -> if (isPureBodyweightSeries) totalReps.toDouble() else totalVolumeKg
        ChartMetric.BEST_REPS -> bestReps.toDouble()
    }
}

data class ExerciseAnalytics(
    val points: List<ExerciseSessionPoint>,
    val isBodyweightExercise: Boolean,
    val isPureBodyweightSeries: Boolean,
    val allTimeBest1RmKg: Double,
    val allTimeMaxWeightKg: Double,
    val allTimeBestVolumeKg: Double,
    val allTimeBestReps: Int,
    val trend: ProgressTrend,
    val trendDeltaPercent: Double?,
    val plateauSessionCount: Int,
    val summaryMessage: String,
    val prPointIndexByMetric: Map<ChartMetric, Int>
)

data class DayCompletionStatus(
    val targetCount: Int,
    val mandatoryTotal: Int,
    val mandatoryCompleted: Int,
    val optionalCompleted: Int,
    val optionalRequired: Int,
    val mandatoryRemaining: Int,
    val optionalRemaining: Int,
    val totalCompleted: Int,
    val extraCompleted: Int,
    val isComplete: Boolean,
    val progressFraction: Float,
    val statusHeadline: String
)

enum class WeekStatus {
    MET,
    MISSED,
    IN_PROGRESS,
    BEFORE_TRACKING,
    FUTURE
}

data class CalendarDayCell(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val isToday: Boolean,
    val sessions: List<WorkoutSession>
) {
    val hasWorkout: Boolean get() = sessions.isNotEmpty()
    val allGoalsMet: Boolean get() = sessions.any { it.isDayGoalMet }
}

data class CalendarWeekSummary(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val daysTrained: Int,
    val weeklyGoal: Int,
    val status: WeekStatus,
    val remainingDaysInWeek: Int,
    val isMathematicallyMissed: Boolean,
    val days: List<CalendarDayCell>
)
