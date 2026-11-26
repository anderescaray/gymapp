package com.kova.app.domain.logic

import com.kova.app.domain.model.CalendarDayCell
import com.kova.app.domain.model.CalendarWeekSummary
import com.kova.app.domain.model.WeekStatus
import com.kova.app.domain.model.WorkoutSession
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

object CalendarEvaluator {

    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekEnd(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    fun buildMonthWeeks(
        month: YearMonth,
        finishedSessions: List<WorkoutSession>,
        weeklyGoal: Int,
        trackingStartDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): List<CalendarWeekSummary> {
        val safeGoal = weeklyGoal.coerceIn(1, 7)
        val sessionsByDate = finishedSessions
            .filter { !it.isActive }
            .groupBy { it.date }

        val firstOfMonth = month.atDay(1)
        val lastOfMonth = month.atEndOfMonth()

        val gridStart = weekStart(firstOfMonth)
        val gridEnd = weekEnd(lastOfMonth)

        val weeks = mutableListOf<CalendarWeekSummary>()
        var cursor = gridStart
        while (!cursor.isAfter(gridEnd)) {
            val wStart = cursor
            val wEnd = cursor.plusDays(6)
            weeks += evaluateWeek(
                weekStart = wStart,
                weekEnd = wEnd,
                targetMonth = month,
                sessionsByDate = sessionsByDate,
                weeklyGoal = safeGoal,
                trackingStartDate = trackingStartDate,
                today = today
            )
            cursor = cursor.plusWeeks(1)
        }
        return weeks
    }

    fun evaluateCurrentWeek(
        finishedSessions: List<WorkoutSession>,
        weeklyGoal: Int,
        trackingStartDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): CalendarWeekSummary {
        val wStart = weekStart(today)
        val wEnd = weekEnd(today)
        val sessionsByDate = finishedSessions
            .filter { !it.isActive }
            .groupBy { it.date }
        return evaluateWeek(
            weekStart = wStart,
            weekEnd = wEnd,
            targetMonth = YearMonth.from(today),
            sessionsByDate = sessionsByDate,
            weeklyGoal = weeklyGoal.coerceIn(1, 7),
            trackingStartDate = trackingStartDate,
            today = today
        )
    }

    private fun evaluateWeek(
        weekStart: LocalDate,
        weekEnd: LocalDate,
        targetMonth: YearMonth,
        sessionsByDate: Map<LocalDate, List<WorkoutSession>>,
        weeklyGoal: Int,
        trackingStartDate: LocalDate,
        today: LocalDate
    ): CalendarWeekSummary {
        val days = (0L..6L).map { offset ->
            val date = weekStart.plusDays(offset)
            CalendarDayCell(
                date = date,
                inCurrentMonth = YearMonth.from(date) == targetMonth,
                isToday = date == today,
                sessions = sessionsByDate[date].orEmpty()
            )
        }

        val daysTrained = days.count { it.hasWorkout }
        val isCurrentWeek = !today.isBefore(weekStart) && !today.isAfter(weekEnd)
        val trainedToday = sessionsByDate[today].orEmpty().isNotEmpty()

        val remainingOpportunities = if (isCurrentWeek) {
            val daysAfterToday = (weekEnd.toEpochDay() - today.toEpochDay()).toInt().coerceAtLeast(0)
            daysAfterToday + if (trainedToday) 0 else 1
        } else 0

        val isPartialStartWeek = trackingStartDate.isAfter(weekStart) && !trackingStartDate.isAfter(weekEnd)
        val availableDaysInStartWeek = if (isPartialStartWeek) {
            (weekEnd.toEpochDay() - trackingStartDate.toEpochDay()).toInt() + 1
        } else 7
        val impossiblePartialStartWeek = isPartialStartWeek && availableDaysInStartWeek < weeklyGoal

        val isMathematicallyMissed = isCurrentWeek &&
            !impossiblePartialStartWeek &&
            daysTrained < weeklyGoal &&
            (weeklyGoal - daysTrained) > remainingOpportunities

        val status = when {
            daysTrained >= weeklyGoal -> WeekStatus.MET
            weekEnd.isBefore(trackingStartDate) && daysTrained == 0 -> WeekStatus.BEFORE_TRACKING
            weekStart.isAfter(today) -> WeekStatus.FUTURE
            weekEnd.isBefore(today) && impossiblePartialStartWeek -> WeekStatus.BEFORE_TRACKING
            weekEnd.isBefore(today) -> WeekStatus.MISSED
            else -> WeekStatus.IN_PROGRESS
        }

        return CalendarWeekSummary(
            weekStart = weekStart,
            weekEnd = weekEnd,
            daysTrained = daysTrained,
            weeklyGoal = weeklyGoal,
            status = status,
            remainingDaysInWeek = remainingOpportunities,
            isMathematicallyMissed = isMathematicallyMissed,
            days = days
        )
    }

    /**
     * Counts consecutive weeks meeting the weekly goal.
     * If the current week is still in progress (not yet met), starts counting from last completed week
     * so an active streak isn't wiped to 0 on Monday morning.
     */
    fun calculateWeeklyStreak(
        finishedSessions: List<WorkoutSession>,
        weeklyGoal: Int,
        trackingStartDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): Int {
        val safeGoal = weeklyGoal.coerceIn(1, 7)
        val trainedDates = finishedSessions
            .asSequence()
            .filter { !it.isActive }
            .map { it.date }
            .toSet()

        if (trainedDates.isEmpty()) return 0

        val currentWeekStart = weekStart(today)
        val earliestDate = minOf(trackingStartDate, trainedDates.min())
        val earliestWeekStart = weekStart(earliestDate)

        fun countDaysInWeek(wStart: LocalDate): Int {
            var count = 0
            for (d in 0L..6L) {
                if (wStart.plusDays(d) in trainedDates) count++
            }
            return count
        }

        var streak = 0
        val currentWeekMet = countDaysInWeek(currentWeekStart) >= safeGoal
        var cursor = if (currentWeekMet) {
            streak = 1
            currentWeekStart.minusWeeks(1)
        } else {
            currentWeekStart.minusWeeks(1)
        }

        while (!cursor.isBefore(earliestWeekStart)) {
            if (countDaysInWeek(cursor) >= safeGoal) {
                streak++
                cursor = cursor.minusWeeks(1)
            } else {
                break
            }
        }
        return streak
    }

    fun countWorkoutsInMonth(
        month: YearMonth,
        finishedSessions: List<WorkoutSession>
    ): Int {
        return finishedSessions
            .asSequence()
            .filter { !it.isActive && YearMonth.from(it.date) == month }
            .map { it.date }
            .distinct()
            .count()
    }
}
