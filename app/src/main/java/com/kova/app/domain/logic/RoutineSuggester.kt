package com.kova.app.domain.logic

import com.kova.app.domain.model.RoutineDay
import com.kova.app.domain.model.WorkoutSession

object RoutineSuggester {

    /**
     * Suggests the next routine day in circular rotation (e.g. Push -> Pull -> Leg -> Push)
     * based on the most recently completed workout session.
     */
    fun suggestNextRoutine(
        routines: List<RoutineDay>,
        finishedSessions: List<WorkoutSession>
    ): RoutineDay? {
        val activeRoutines = routines
            .filterNot { it.isArchived }
            .sortedWith(compareBy<RoutineDay> { it.sortOrder }.thenBy { it.id })
        if (activeRoutines.isEmpty()) return null

        val lastSession = finishedSessions
            .filter { !it.isActive }
            .maxWithOrNull(
                compareBy<WorkoutSession> { it.dateEpochDay }
                    .thenBy { it.finishedAtEpochMillis ?: it.startedAtEpochMillis }
                    .thenBy { it.id }
            ) ?: return activeRoutines.first()

        val lastIndex = activeRoutines.indexOfFirst { it.id == lastSession.routineDayId }
        if (lastIndex == -1) return activeRoutines.first()

        val nextIndex = (lastIndex + 1) % activeRoutines.size
        return activeRoutines[nextIndex]
    }
}
