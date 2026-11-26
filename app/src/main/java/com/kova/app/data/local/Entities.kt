package com.kova.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kova.app.domain.model.Exercise
import com.kova.app.domain.model.RoutineDay
import com.kova.app.domain.model.WorkoutSession
import com.kova.app.domain.model.WorkoutSet

@Entity(tableName = "routine_days")
data class RoutineDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorHex: String,
    val targetExerciseCount: Int,
    val sortOrder: Int,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long
)

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineDayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineDayId")]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineDayId: Long,
    val name: String,
    val isMandatory: Boolean,
    val isBodyweight: Boolean,
    val notes: String,
    val defaultRestSeconds: Int?,
    val manualOneRmKg: Double? = null,
    val manualOneRmDateEpochDay: Long? = null,
    val sortOrder: Int,
    val isArchived: Boolean = false,
    val createdAtEpochMillis: Long
)

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = RoutineDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineDayId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("routineDayId"),
        Index("dateEpochDay")
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val routineDayId: Long?,
    val routineDayNameSnapshot: String,
    val routineColorHexSnapshot: String,
    val dateEpochDay: Long,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long?,
    val targetExerciseCountSnapshot: Int,
    val mandatoryCountSnapshot: Int,
    val completedExerciseCount: Int,
    val allMandatoryCompleted: Boolean,
    val isDayGoalMet: Boolean,
    val sessionNote: String
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("sessionId"),
        Index("exerciseId")
    ]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val exerciseId: Long?,
    val exerciseNameSnapshot: String,
    val isMandatorySnapshot: Boolean,
    val isBodyweightSnapshot: Boolean,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val isCompleted: Boolean,
    val completedAtEpochMillis: Long?
)

fun RoutineDayEntity.toDomain() = RoutineDay(
    id = id,
    name = name,
    colorHex = colorHex,
    targetExerciseCount = targetExerciseCount,
    sortOrder = sortOrder,
    isArchived = isArchived,
    createdAtEpochMillis = createdAtEpochMillis
)

fun RoutineDay.toEntity() = RoutineDayEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    targetExerciseCount = targetExerciseCount,
    sortOrder = sortOrder,
    isArchived = isArchived,
    createdAtEpochMillis = createdAtEpochMillis
)

fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    routineDayId = routineDayId,
    name = name,
    isMandatory = isMandatory,
    isBodyweight = isBodyweight,
    notes = notes,
    defaultRestSeconds = defaultRestSeconds,
    manualOneRmKg = manualOneRmKg,
    manualOneRmDateEpochDay = manualOneRmDateEpochDay,
    sortOrder = sortOrder,
    isArchived = isArchived,
    createdAtEpochMillis = createdAtEpochMillis
)

fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    routineDayId = routineDayId,
    name = name,
    isMandatory = isMandatory,
    isBodyweight = isBodyweight,
    notes = notes,
    defaultRestSeconds = defaultRestSeconds,
    manualOneRmKg = manualOneRmKg,
    manualOneRmDateEpochDay = manualOneRmDateEpochDay,
    sortOrder = sortOrder,
    isArchived = isArchived,
    createdAtEpochMillis = createdAtEpochMillis
)

fun WorkoutSessionEntity.toDomain() = WorkoutSession(
    id = id,
    routineDayId = routineDayId,
    routineDayNameSnapshot = routineDayNameSnapshot,
    routineColorHexSnapshot = routineColorHexSnapshot,
    dateEpochDay = dateEpochDay,
    startedAtEpochMillis = startedAtEpochMillis,
    finishedAtEpochMillis = finishedAtEpochMillis,
    targetExerciseCountSnapshot = targetExerciseCountSnapshot,
    mandatoryCountSnapshot = mandatoryCountSnapshot,
    completedExerciseCount = completedExerciseCount,
    allMandatoryCompleted = allMandatoryCompleted,
    isDayGoalMet = isDayGoalMet,
    sessionNote = sessionNote
)

fun WorkoutSession.toEntity() = WorkoutSessionEntity(
    id = id,
    routineDayId = routineDayId,
    routineDayNameSnapshot = routineDayNameSnapshot,
    routineColorHexSnapshot = routineColorHexSnapshot,
    dateEpochDay = dateEpochDay,
    startedAtEpochMillis = startedAtEpochMillis,
    finishedAtEpochMillis = finishedAtEpochMillis,
    targetExerciseCountSnapshot = targetExerciseCountSnapshot,
    mandatoryCountSnapshot = mandatoryCountSnapshot,
    completedExerciseCount = completedExerciseCount,
    allMandatoryCompleted = allMandatoryCompleted,
    isDayGoalMet = isDayGoalMet,
    sessionNote = sessionNote
)

fun WorkoutSetEntity.toDomain() = WorkoutSet(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    exerciseNameSnapshot = exerciseNameSnapshot,
    isMandatorySnapshot = isMandatorySnapshot,
    isBodyweightSnapshot = isBodyweightSnapshot,
    setIndex = setIndex,
    weightKg = weightKg,
    reps = reps,
    isCompleted = isCompleted,
    completedAtEpochMillis = completedAtEpochMillis
)

fun WorkoutSet.toEntity() = WorkoutSetEntity(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    exerciseNameSnapshot = exerciseNameSnapshot,
    isMandatorySnapshot = isMandatorySnapshot,
    isBodyweightSnapshot = isBodyweightSnapshot,
    setIndex = setIndex,
    weightKg = weightKg,
    reps = reps,
    isCompleted = isCompleted,
    completedAtEpochMillis = completedAtEpochMillis
)
