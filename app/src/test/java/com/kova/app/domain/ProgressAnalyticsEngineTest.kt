package com.kova.app.domain

import com.kova.app.domain.logic.ProgressAnalyticsEngine
import com.kova.app.domain.model.ChartMetric
import com.kova.app.domain.model.ProgressTrend
import com.kova.app.domain.model.WorkoutSession
import com.kova.app.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgressAnalyticsEngineTest {

    private fun sessionOn(id: Long, date: LocalDate) = WorkoutSession(
        id = id,
        routineDayId = 1,
        routineDayNameSnapshot = "Push",
        routineColorHexSnapshot = "#C6F635",
        dateEpochDay = date.toEpochDay(),
        startedAtEpochMillis = id * 1000L,
        finishedAtEpochMillis = id * 1000L + 3_000_000L,
        targetExerciseCountSnapshot = 5,
        mandatoryCountSnapshot = 4
    )

    private fun setFor(sessionId: Long, weightKg: Double, reps: Int, bw: Boolean = false) = WorkoutSet(
        id = sessionId * 100,
        sessionId = sessionId,
        exerciseId = 10,
        exerciseNameSnapshot = "Press banca",
        isMandatorySnapshot = true,
        isBodyweightSnapshot = bw,
        setIndex = 1,
        weightKg = weightKg,
        reps = reps,
        isCompleted = true
    )

    @Test
    fun `epley 1RM formula computes accurate values`() {
        assertEquals(80.0, ProgressAnalyticsEngine.estimate1RmKg(80.0, 1), 0.01)
        assertEquals(96.0, ProgressAnalyticsEngine.estimate1RmKg(80.0, 6), 0.01)
        assertEquals(106.67, ProgressAnalyticsEngine.estimate1RmKg(80.0, 10), 0.02)
        assertEquals(0.0, ProgressAnalyticsEngine.estimate1RmKg(0.0, 10), 0.01)
    }

    @Test
    fun `detects NEW_PR on steady progressive overload`() {
        val base = LocalDate.of(2026, 10, 1)
        val history = listOf(
            sessionOn(1, base) to listOf(setFor(1, 70.0, 8)),
            sessionOn(2, base.plusDays(4)) to listOf(setFor(2, 72.5, 8)),
            sessionOn(3, base.plusDays(8)) to listOf(setFor(3, 75.0, 8))
        )

        val analytics = ProgressAnalyticsEngine.analyzeExercise(history, isBodyweightExercise = false)

        assertEquals(ProgressTrend.NEW_PR, analytics.trend)
        assertEquals(75.0, analytics.allTimeMaxWeightKg, 0.01)
        assertEquals(2, analytics.prPointIndexByMetric[ChartMetric.ESTIMATED_1RM])
    }

    @Test
    fun `detects PLATEAU after 3 consecutive identical sessions`() {
        val base = LocalDate.of(2026, 10, 1)
        val history = listOf(
            sessionOn(1, base) to listOf(setFor(1, 42.5, 8)),
            sessionOn(2, base.plusDays(4)) to listOf(setFor(2, 45.0, 8)),
            sessionOn(3, base.plusDays(8)) to listOf(setFor(3, 45.0, 8)),
            sessionOn(4, base.plusDays(12)) to listOf(setFor(4, 45.0, 8))
        )

        val analytics = ProgressAnalyticsEngine.analyzeExercise(history, isBodyweightExercise = false)

        assertEquals(ProgressTrend.PLATEAU, analytics.trend)
        assertTrue(analytics.plateauSessionCount >= 3)
        assertTrue(analytics.summaryMessage.contains("sesiones"))
    }

    @Test
    fun `handles pure bodyweight and weighted bodyweight progression smoothly`() {
        val base = LocalDate.of(2026, 10, 1)
        val history = listOf(
            sessionOn(1, base) to listOf(setFor(1, 0.0, 6, bw = true)),
            sessionOn(2, base.plusDays(5)) to listOf(setFor(2, 0.0, 9, bw = true)),
            sessionOn(3, base.plusDays(10)) to listOf(setFor(3, 5.0, 8, bw = true)),
            sessionOn(4, base.plusDays(15)) to listOf(setFor(4, 7.5, 8, bw = true))
        )

        val analytics = ProgressAnalyticsEngine.analyzeExercise(history, isBodyweightExercise = true)

        assertEquals(ProgressTrend.NEW_PR, analytics.trend)
        assertTrue(analytics.isBodyweightExercise)
        assertFalse(analytics.isPureBodyweightSeries)
        assertEquals(7.5, analytics.allTimeMaxWeightKg, 0.01)
        // Ensure ESTIMATED_1RM axis climbs monotonically without dropping when adding weight (+5 kg)
        val vals = analytics.points.map { it.valueFor(ChartMetric.ESTIMATED_1RM, analytics.isPureBodyweightSeries) }
        assertTrue(vals[3] > vals[2] && vals[2] > vals[0])
        assertEquals(3, analytics.prPointIndexByMetric[ChartMetric.ESTIMATED_1RM])
    }

    @Test
    fun `live PR checker returns true only when beating previous historical best`() {
        val prior = listOf(
            setFor(1, 70.0, 8), // score ~88.67
            setFor(2, 72.5, 6)  // score ~87.0
        )

        assertTrue(ProgressAnalyticsEngine.isSetNewPr(72.5, 8, false, prior))
        assertFalse(ProgressAnalyticsEngine.isSetNewPr(70.0, 8, false, prior))
        assertFalse(ProgressAnalyticsEngine.isSetNewPr(65.0, 8, false, prior))
    }
}
