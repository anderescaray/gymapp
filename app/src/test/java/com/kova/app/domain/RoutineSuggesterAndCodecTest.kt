package com.kova.app.domain

import com.kova.app.domain.logic.RoutineSuggester
import com.kova.app.domain.logic.WeightFormatter
import com.kova.app.domain.model.AccentPalette
import com.kova.app.domain.model.Exercise
import com.kova.app.domain.model.RoutineDay
import com.kova.app.domain.model.ThemeMode
import com.kova.app.domain.model.UserPreferences
import com.kova.app.domain.model.WeightUnit
import com.kova.app.domain.model.WorkoutSession
import com.kova.app.domain.model.WorkoutSet
import com.kova.app.domain.sync.KovaBackupCodec
import com.kova.app.domain.sync.KovaBackupSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RoutineSuggesterAndCodecTest {

    @Test
    fun `suggestNextRoutine cycles Push to Pull to Leg to Push`() {
        val push = RoutineDay(id = 1, name = "Push", sortOrder = 0)
        val pull = RoutineDay(id = 2, name = "Pull", sortOrder = 1)
        val leg = RoutineDay(id = 3, name = "Leg", sortOrder = 2)
        val all = listOf(push, pull, leg)

        assertEquals(push, RoutineSuggester.suggestNextRoutine(all, emptyList()))

        val afterPush = listOf(
            WorkoutSession(
                id = 10, routineDayId = 1, routineDayNameSnapshot = "Push",
                routineColorHexSnapshot = "#C6F635",
                dateEpochDay = LocalDate.of(2026, 11, 20).toEpochDay(),
                startedAtEpochMillis = 1000L, finishedAtEpochMillis = 2000L,
                targetExerciseCountSnapshot = 5, mandatoryCountSnapshot = 4
            )
        )
        assertEquals(pull, RoutineSuggester.suggestNextRoutine(all, afterPush))

        val afterLeg = afterPush + WorkoutSession(
            id = 11, routineDayId = 3, routineDayNameSnapshot = "Leg",
            routineColorHexSnapshot = "#FF6B4A",
            dateEpochDay = LocalDate.of(2026, 11, 22).toEpochDay(),
            startedAtEpochMillis = 3000L, finishedAtEpochMillis = 4000L,
            targetExerciseCountSnapshot = 4, mandatoryCountSnapshot = 3
        )
        assertEquals(push, RoutineSuggester.suggestNextRoutine(all, afterLeg))
    }

    @Test
    fun `weight formatter formats kg, lb, and bodyweight cleanly`() {
        assertEquals("70 kg", WeightFormatter.formatWeight(70.0, WeightUnit.KG, false))
        assertEquals("72.5 kg", WeightFormatter.formatWeight(72.5, WeightUnit.KG, false))
        assertEquals("PC", WeightFormatter.formatWeight(0.0, WeightUnit.KG, true))
        assertEquals("PC +10 kg", WeightFormatter.formatWeight(10.0, WeightUnit.KG, true))
        assertEquals(72.5, WeightFormatter.stepWeightKg(70.0, +1, WeightUnit.KG), 0.01)
        assertEquals(0.0, WeightFormatter.stepWeightKg(1.0, -1, WeightUnit.KG), 0.01)
    }

    @Test
    fun `backup codec round-trips full snapshot without data loss`() {
        val original = KovaBackupSnapshot(
            exportedAtEpochMillis = 1795460000000L,
            preferences = UserPreferences(
                hasCompletedOnboarding = true,
                trackingStartEpochDay = 20700L,
                weeklyGoalDays = 4,
                weightUnit = WeightUnit.KG,
                themeMode = ThemeMode.DARK,
                accentPalette = AccentPalette.CORAL,
                defaultRestSeconds = 120,
                autoStartRestTimer = true,
                soundAndHapticsEnabled = false
            ),
            routineDays = listOf(
                RoutineDay(id = 1, name = "Push", colorHex = "#C6F635", targetExerciseCount = 5, sortOrder = 0)
            ),
            exercises = listOf(
                Exercise(
                    id = 10, routineDayId = 1, name = "Press banca",
                    isMandatory = true, isBodyweight = false, notes = "Agarre ancho", defaultRestSeconds = 120
                )
            ),
            workoutSessions = listOf(
                WorkoutSession(
                    id = 100, routineDayId = 1, routineDayNameSnapshot = "Push",
                    routineColorHexSnapshot = "#C6F635", dateEpochDay = 20750L,
                    startedAtEpochMillis = 1000L, finishedAtEpochMillis = 4000L,
                    targetExerciseCountSnapshot = 5, mandatoryCountSnapshot = 4,
                    completedExerciseCount = 5, allMandatoryCompleted = true, isDayGoalMet = true
                )
            ),
            workoutSets = listOf(
                WorkoutSet(
                    id = 1000, sessionId = 100, exerciseId = 10,
                    exerciseNameSnapshot = "Press banca", isMandatorySnapshot = true,
                    isBodyweightSnapshot = false, setIndex = 1, weightKg = 75.0, reps = 6,
                    isCompleted = true, completedAtEpochMillis = 2500L
                )
            )
        )

        val encoded = KovaBackupCodec.encode(original)
        val decoded = KovaBackupCodec.decode(encoded)

        assertTrue(decoded.isSuccess)
        val restored = decoded.getOrThrow()
        assertEquals(original.preferences.weeklyGoalDays, restored.preferences.weeklyGoalDays)
        assertEquals(original.preferences.accentPalette, restored.preferences.accentPalette)
        assertEquals(original.preferences.themeMode, restored.preferences.themeMode)
        assertEquals(1, restored.routineDays.size)
        assertEquals("Push", restored.routineDays.first().name)
        assertEquals("Agarre ancho", restored.exercises.first().notes)
        assertEquals(75.0, restored.workoutSets.first().weightKg, 0.01)
    }

    @Test
    fun `backup codec rejects unsupported schema version`() {
        val bad = """{"schemaVersion": 99}"""
        assertTrue(KovaBackupCodec.decode(bad).isFailure)
    }

    @Test
    fun `backup codec sanitizes non-finite or negative weights cleanly`() {
        val json = """
            {
              "schemaVersion": 1,
              "preferences": {"weeklyGoalDays": 4},
              "routineDays": [],
              "exercises": [],
              "workoutSessions": [],
              "workoutSets": [
                {"id": 1, "sessionId": 1, "exerciseNameSnapshot": "Test", "setIndex": 0, "weightKg": -50.0, "reps": -5}
              ]
            }
        """.trimIndent()
        val restored = KovaBackupCodec.decode(json).getOrThrow()
        assertEquals(0.0, restored.workoutSets.first().weightKg, 0.001)
        assertEquals(0, restored.workoutSets.first().reps)
        assertEquals(1, restored.workoutSets.first().setIndex)
    }

    @Test
    fun `backup codec round-trips ROSE accent palette and 180s exercise rest`() {
        val original = KovaBackupSnapshot(
            exportedAtEpochMillis = 1795460000000L,
            preferences = UserPreferences(
                hasCompletedOnboarding = true,
                accentPalette = AccentPalette.ROSE,
                defaultRestSeconds = 90
            ),
            routineDays = listOf(RoutineDay(id = 1, name = "Push", targetExerciseCount = 4)),
            exercises = listOf(
                Exercise(
                    id = 1,
                    routineDayId = 1,
                    name = "Press militar",
                    isMandatory = true,
                    defaultRestSeconds = 180,
                    manualOneRmKg = 60.0,
                    manualOneRmDateEpochDay = 20780L
                )
            ),
            workoutSessions = emptyList(),
            workoutSets = emptyList()
        )
        val restored = KovaBackupCodec.decode(KovaBackupCodec.encode(original)).getOrThrow()
        assertEquals(AccentPalette.ROSE, restored.preferences.accentPalette)
        assertEquals(180, restored.exercises.first().defaultRestSeconds)
        assertEquals(60.0, restored.exercises.first().manualOneRmKg!!, 0.001)
        assertEquals(20780L, restored.exercises.first().manualOneRmDateEpochDay)
    }
}
