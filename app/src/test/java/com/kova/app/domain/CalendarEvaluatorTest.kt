package com.kova.app.domain

import com.kova.app.domain.logic.CalendarEvaluator
import com.kova.app.domain.model.WeekStatus
import com.kova.app.domain.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarEvaluatorTest {

    private fun finishedSession(id: Long, date: LocalDate) = WorkoutSession(
        id = id,
        routineDayId = 1,
        routineDayNameSnapshot = "Push",
        routineColorHexSnapshot = "#C6F635",
        dateEpochDay = date.toEpochDay(),
        startedAtEpochMillis = id * 1000L,
        finishedAtEpochMillis = id * 1000L + 3_000_000L,
        targetExerciseCountSnapshot = 5,
        mandatoryCountSnapshot = 4,
        completedExerciseCount = 5,
        allMandatoryCompleted = true,
        isDayGoalMet = true
    )

    @Test
    fun `marks past weeks MET when goal hit, MISSED when below goal, and BEFORE_TRACKING before start`() {
        // Tuesday Nov 24, 2026
        val today = LocalDate.of(2026, 11, 24)
        // Tracking started Monday Nov 9, 2026
        val trackingStart = LocalDate.of(2026, 11, 9)

        val sessions = listOf(
            // Week Nov 9..15 -> 3 workouts (goal = 3 => MET!)
            finishedSession(1, LocalDate.of(2026, 11, 9)),
            finishedSession(2, LocalDate.of(2026, 11, 11)),
            finishedSession(3, LocalDate.of(2026, 11, 14)),
            // Week Nov 16..22 -> 1 workout (goal = 3 => MISSED!)
            finishedSession(4, LocalDate.of(2026, 11, 17)),
            // Current week Nov 23..29 -> 1 workout so far (IN_PROGRESS!)
            finishedSession(5, LocalDate.of(2026, 11, 23))
        )

        val weeks = CalendarEvaluator.buildMonthWeeks(
            month = YearMonth.of(2026, 11),
            finishedSessions = sessions,
            weeklyGoal = 3,
            trackingStartDate = trackingStart,
            today = today
        )

        assertTrue(weeks.all { it.weekStart.dayOfWeek == DayOfWeek.MONDAY })
        assertTrue(weeks.all { it.weekEnd.dayOfWeek == DayOfWeek.SUNDAY })

        val weekNov2 = weeks.first { it.weekStart == LocalDate.of(2026, 11, 2) }
        val weekNov9 = weeks.first { it.weekStart == LocalDate.of(2026, 11, 9) }
        val weekNov16 = weeks.first { it.weekStart == LocalDate.of(2026, 11, 16) }
        val weekNov23 = weeks.first { it.weekStart == LocalDate.of(2026, 11, 23) }

        assertEquals(WeekStatus.BEFORE_TRACKING, weekNov2.status)
        assertEquals(WeekStatus.MET, weekNov9.status)
        assertEquals(WeekStatus.MISSED, weekNov16.status)
        assertEquals(WeekStatus.IN_PROGRESS, weekNov23.status)
    }

    @Test
    fun `streak stays alive on Monday morning when current week is still in progress`() {
        val today = LocalDate.of(2026, 11, 23) // Monday
        val trackingStart = LocalDate.of(2026, 11, 2)

        val sessions = listOf(
            // Week Nov 2..8: 3 days (MET)
            finishedSession(1, LocalDate.of(2026, 11, 2)),
            finishedSession(2, LocalDate.of(2026, 11, 4)),
            finishedSession(3, LocalDate.of(2026, 11, 6)),
            // Week Nov 9..15: 3 days (MET)
            finishedSession(4, LocalDate.of(2026, 11, 9)),
            finishedSession(5, LocalDate.of(2026, 11, 11)),
            finishedSession(6, LocalDate.of(2026, 11, 13)),
            // Week Nov 16..22: 3 days (MET)
            finishedSession(7, LocalDate.of(2026, 11, 16)),
            finishedSession(8, LocalDate.of(2026, 11, 18)),
            finishedSession(9, LocalDate.of(2026, 11, 20))
        )

        val streak = CalendarEvaluator.calculateWeeklyStreak(
            finishedSessions = sessions,
            weeklyGoal = 3,
            trackingStartDate = trackingStart,
            today = today
        )
        assertEquals(3, streak)
    }

    @Test
    fun `multiple sessions on the same calendar day count as 1 unique gym day`() {
        val today = LocalDate.of(2026, 11, 25) // Wednesday
        val sessions = listOf(
            finishedSession(1, LocalDate.of(2026, 11, 23)),
            finishedSession(2, LocalDate.of(2026, 11, 23)), // same day
            finishedSession(3, LocalDate.of(2026, 11, 24))
        )

        val currentWeek = CalendarEvaluator.evaluateCurrentWeek(
            finishedSessions = sessions,
            weeklyGoal = 3,
            trackingStartDate = LocalDate.of(2026, 11, 1),
            today = today
        )
        assertEquals(2, currentWeek.daysTrained)
        assertEquals(WeekStatus.IN_PROGRESS, currentWeek.status)
    }

    @Test
    fun `partial start week with fewer remaining days than weeklyGoal does not turn red`() {
        // User started tracking on Saturday Nov 14 with goal = 4 (only 2 days left in week Nov 9..15)
        val trackingStart = LocalDate.of(2026, 11, 14)
        val today = LocalDate.of(2026, 11, 20)
        val weeks = CalendarEvaluator.buildMonthWeeks(
            month = YearMonth.of(2026, 11),
            finishedSessions = listOf(finishedSession(1, LocalDate.of(2026, 11, 14))),
            weeklyGoal = 4,
            trackingStartDate = trackingStart,
            today = today
        )
        val startWeek = weeks.first { it.weekStart == LocalDate.of(2026, 11, 9) }
        assertEquals(WeekStatus.BEFORE_TRACKING, startWeek.status)
    }
}
