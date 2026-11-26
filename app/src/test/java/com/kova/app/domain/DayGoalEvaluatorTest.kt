package com.kova.app.domain

import com.kova.app.domain.logic.DayGoalEvaluator
import com.kova.app.domain.model.Exercise
import com.kova.app.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayGoalEvaluatorTest {

    private val routineExercises = listOf(
        Exercise(id = 1, routineDayId = 1, name = "Press banca", isMandatory = true),
        Exercise(id = 2, routineDayId = 1, name = "Press inclinado", isMandatory = true),
        Exercise(id = 3, routineDayId = 1, name = "Press militar", isMandatory = true),
        Exercise(id = 4, routineDayId = 1, name = "Elevaciones laterales", isMandatory = true),
        Exercise(id = 5, routineDayId = 1, name = "Fondos", isMandatory = false),
        Exercise(id = 6, routineDayId = 1, name = "Cruces", isMandatory = false),
        Exercise(id = 7, routineDayId = 1, name = "Tríceps cuerda", isMandatory = false)
    )

    private fun doneSet(exerciseId: Long, reps: Int = 8, completed: Boolean = true) = WorkoutSet(
        id = exerciseId * 10,
        sessionId = 1,
        exerciseId = exerciseId,
        exerciseNameSnapshot = "Ex $exerciseId",
        isMandatorySnapshot = exerciseId <= 4,
        isBodyweightSnapshot = false,
        setIndex = 1,
        weightKg = 60.0,
        reps = reps,
        isCompleted = completed
    )

    @Test
    fun `completes day when all 4 mandatory and 1 optional are done for target 5`() {
        val sets = listOf(doneSet(1), doneSet(2), doneSet(3), doneSet(4), doneSet(5))
        val status = DayGoalEvaluator.evaluate(routineExercises, targetExerciseCount = 5, sessionSets = sets)

        assertTrue(status.isComplete)
        assertEquals(4, status.mandatoryCompleted)
        assertEquals(1, status.optionalCompleted)
        assertEquals(0, status.mandatoryRemaining)
        assertEquals(0, status.optionalRemaining)
        assertEquals(5, status.totalCompleted)
        assertEquals(1.0f, status.progressFraction, 0.001f)
    }

    @Test
    fun `does NOT complete day when 3 mandatory and 2 optional are done even though total is 5`() {
        val sets = listOf(doneSet(1), doneSet(2), doneSet(3), doneSet(5), doneSet(6))
        val status = DayGoalEvaluator.evaluate(routineExercises, targetExerciseCount = 5, sessionSets = sets)

        assertFalse(status.isComplete)
        assertEquals(3, status.mandatoryCompleted)
        assertEquals(1, status.mandatoryRemaining)
        assertEquals(0, status.optionalRemaining)
        assertEquals(5, status.totalCompleted)
        assertTrue(status.progressFraction < 1.0f)
        assertTrue(status.statusHeadline.contains("1 obligatorio"))
    }

    @Test
    fun `prompts for 1 optional when all 4 mandatory are finished`() {
        val sets = listOf(doneSet(1), doneSet(2), doneSet(3), doneSet(4))
        val status = DayGoalEvaluator.evaluate(routineExercises, targetExerciseCount = 5, sessionSets = sets)

        assertFalse(status.isComplete)
        assertEquals(0, status.mandatoryRemaining)
        assertEquals(1, status.optionalRemaining)
        assertEquals(0.8f, status.progressFraction, 0.001f)
        assertTrue(status.statusHeadline.contains("1 opcional"))
    }

    @Test
    fun `counts bonus optional exercises as extra after completing goal`() {
        val sets = listOf(doneSet(1), doneSet(2), doneSet(3), doneSet(4), doneSet(5), doneSet(6))
        val status = DayGoalEvaluator.evaluate(routineExercises, targetExerciseCount = 5, sessionSets = sets)

        assertTrue(status.isComplete)
        assertEquals(1, status.extraCompleted)
        assertTrue(status.statusHeadline.contains("+1 extra"))
    }

    @Test
    fun `ignores uncompleted draft sets and zero-rep sets`() {
        val sets = listOf(
            doneSet(1, reps = 8, completed = false),
            doneSet(2, reps = 0, completed = true),
            doneSet(3, reps = 10, completed = true)
        )
        val status = DayGoalEvaluator.evaluate(routineExercises, targetExerciseCount = 5, sessionSets = sets)

        assertFalse(status.isComplete)
        assertEquals(1, status.mandatoryCompleted)
        assertEquals(1, status.totalCompleted)
    }
}
