package com.kova.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDayDao {
    @Query("SELECT * FROM routine_days WHERE isArchived = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeActiveRoutines(): Flow<List<RoutineDayEntity>>

    @Query("SELECT * FROM routine_days ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllRoutinesOnce(): List<RoutineDayEntity>

    @Query("SELECT * FROM routine_days WHERE id = :id")
    suspend fun getById(id: Long): RoutineDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RoutineDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<RoutineDayEntity>)

    @Update
    suspend fun update(entity: RoutineDayEntity)

    @Query("UPDATE routine_days SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("DELETE FROM routine_days")
    suspend fun deleteAll()
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE isArchived = 0 ORDER BY isMandatory DESC, sortOrder ASC, id ASC")
    fun observeAllActiveExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE routineDayId = :routineDayId AND isArchived = 0 ORDER BY isMandatory DESC, sortOrder ASC, id ASC")
    fun observeExercisesForRoutine(routineDayId: Long): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE routineDayId = :routineDayId AND isArchived = 0 ORDER BY isMandatory DESC, sortOrder ASC, id ASC")
    suspend fun getExercisesForRoutineOnce(routineDayId: Long): List<ExerciseEntity>

    @Query("SELECT * FROM exercises ORDER BY routineDayId ASC, sortOrder ASC, id ASC")
    suspend fun getAllExercisesOnce(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: Long): Flow<ExerciseEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ExerciseEntity>)

    @Update
    suspend fun update(entity: ExerciseEntity)

    @Query("UPDATE exercises SET notes = :notes WHERE id = :exerciseId")
    suspend fun updateNotes(exerciseId: Long, notes: String)

    @Query("UPDATE exercises SET manualOneRmKg = :oneRmKg, manualOneRmDateEpochDay = :dateEpochDay WHERE id = :exerciseId")
    suspend fun updateManualOneRm(exerciseId: Long, oneRmKg: Double?, dateEpochDay: Long?)

    @Query("UPDATE exercises SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("DELETE FROM exercises")
    suspend fun deleteAll()
}

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY dateEpochDay DESC, startedAtEpochMillis DESC, id DESC")
    fun observeAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE finishedAtEpochMillis IS NULL ORDER BY startedAtEpochMillis DESC LIMIT 1")
    fun observeActiveSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE finishedAtEpochMillis IS NULL ORDER BY startedAtEpochMillis DESC LIMIT 1")
    suspend fun getActiveSessionOnce(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions ORDER BY dateEpochDay ASC, id ASC")
    suspend fun getAllSessionsOnce(): List<WorkoutSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WorkoutSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<WorkoutSessionEntity>)

    @Update
    suspend fun update(entity: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: Long)

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAll()
}

@Dao
interface WorkoutSetDao {
    @Query("SELECT * FROM workout_sets ORDER BY sessionId ASC, setIndex ASC, id ASC")
    fun observeAllSets(): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY exerciseId ASC, setIndex ASC, id ASC")
    fun observeSetsForSession(sessionId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY exerciseId ASC, setIndex ASC, id ASC")
    suspend fun getSetsForSessionOnce(sessionId: Long): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE exerciseId = :exerciseId ORDER BY sessionId ASC, setIndex ASC, id ASC")
    fun observeSetsForExercise(exerciseId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets ORDER BY sessionId ASC, setIndex ASC, id ASC")
    suspend fun getAllSetsOnce(): List<WorkoutSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WorkoutSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<WorkoutSetEntity>)

    @Update
    suspend fun update(entity: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE id = :setId")
    suspend fun deleteById(setId: Long)

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId AND (isCompleted = 0 OR reps <= 0)")
    suspend fun deleteIncompleteForSession(sessionId: Long)

    @Query("DELETE FROM workout_sets")
    suspend fun deleteAll()
}
