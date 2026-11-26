package com.kova.app.domain.logic

import com.kova.app.domain.model.DayCompletionStatus
import com.kova.app.domain.model.Exercise
import com.kova.app.domain.model.WorkoutSet

object DayGoalEvaluator {

    /**
     * Evaluates daily completion strictly enforcing:
     * 1. ALL mandatory exercises must have at least 1 completed set (reps > 0).
     * 2. Total completed exercises (mandatory + optional) must reach targetExerciseCount
     *    (at minimum equal to the number of mandatory exercises).
     *
     * Example: target = 5, mandatory = 4, optional = 3 -> must complete all 4 mandatory + at least 1 optional.
     */
    fun evaluate(
        routineExercises: List<Exercise>,
        targetExerciseCount: Int,
        sessionSets: List<WorkoutSet>
    ): DayCompletionStatus {
        val active = routineExercises.filterNot { it.isArchived }
        val completedExerciseIds = sessionSets
            .asSequence()
            .filter { it.isCompleted && it.reps > 0 && it.exerciseId != null }
            .mapNotNull { it.exerciseId }
            .toSet()

        val mandatoryIds = active.filter { it.isMandatory }.map { it.id }.toSet()
        val optionalIds = active.filterNot { it.isMandatory }.map { it.id }.toSet()

        val mandatoryTotal = mandatoryIds.size
        val mandatoryCompleted = mandatoryIds.count { it in completedExerciseIds }
        val optionalCompleted = optionalIds.count { it in completedExerciseIds }

        // Ad-hoc exercises logged during session that aren't in the routine list count as optional extras
        val adHocCompleted = completedExerciseIds.count { it !in mandatoryIds && it !in optionalIds }
        val totalOptionalCompleted = optionalCompleted + adHocCompleted

        val effectiveTarget = maxOf(targetExerciseCount.coerceAtLeast(0), mandatoryTotal)
        val optionalRequired = (effectiveTarget - mandatoryTotal).coerceAtLeast(0)
        val mandatoryRemaining = (mandatoryTotal - mandatoryCompleted).coerceAtLeast(0)
        val optionalRemaining = (optionalRequired - totalOptionalCompleted).coerceAtLeast(0)

        val totalCompleted = mandatoryCompleted + totalOptionalCompleted
        val isComplete = effectiveTarget > 0 &&
            mandatoryRemaining == 0 &&
            totalCompleted >= effectiveTarget

        val extraCompleted = if (isComplete) (totalCompleted - effectiveTarget).coerceAtLeast(0) else 0

        // Progress counts mandatory done + capped optionals needed so doing 5 optionals without mandatory doesn't show 100%
        val cappedOptionalContribution = totalOptionalCompleted.coerceAtMost(optionalRequired)
        val effectiveProgressCount = mandatoryCompleted + cappedOptionalContribution
        val progressFraction = if (effectiveTarget <= 0) {
            0f
        } else {
            (effectiveProgressCount.toFloat() / effectiveTarget.toFloat()).coerceIn(0f, 1f)
        }

        val headline = when {
            effectiveTarget == 0 -> "Añade ejercicios a esta rutina para fijar tu objetivo"
            isComplete && extraCompleted > 0 -> "¡Objetivo superado! (+$extraCompleted extra${if (extraCompleted > 1) "s" else ""})"
            isComplete -> "¡Objetivo del día completado!"
            mandatoryRemaining > 0 && optionalRemaining > 0 -> {
                val mTxt = if (mandatoryRemaining == 1) "1 obligatorio" else "$mandatoryRemaining obligatorios"
                val oTxt = if (optionalRemaining == 1) "1 opcional" else "$optionalRemaining opcionales"
                "Faltan $mTxt y $oTxt"
            }
            mandatoryRemaining > 0 -> {
                if (mandatoryRemaining == 1) "Te falta 1 obligatorio para completar el día"
                else "Te faltan $mandatoryRemaining obligatorios para completar el día"
            }
            optionalRemaining > 0 -> {
                val prefix = if (mandatoryTotal > 0) "Obligatorios listos · " else ""
                if (optionalRemaining == 1) "${prefix}Elige 1 opcional más"
                else "${prefix}Elige $optionalRemaining opcionales más"
            }
            else -> "Sigue sumando ejercicios"
        }

        return DayCompletionStatus(
            targetCount = effectiveTarget,
            mandatoryTotal = mandatoryTotal,
            mandatoryCompleted = mandatoryCompleted,
            optionalCompleted = totalOptionalCompleted,
            optionalRequired = optionalRequired,
            mandatoryRemaining = mandatoryRemaining,
            optionalRemaining = optionalRemaining,
            totalCompleted = totalCompleted,
            extraCompleted = extraCompleted,
            isComplete = isComplete,
            progressFraction = progressFraction,
            statusHeadline = headline
        )
    }

    fun isExerciseCompleted(exerciseId: Long, sessionSets: List<WorkoutSet>): Boolean {
        return sessionSets.any { it.exerciseId == exerciseId && it.isCompleted && it.reps > 0 }
    }
}
