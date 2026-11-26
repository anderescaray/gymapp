package com.kova.app.domain.logic

import com.kova.app.domain.model.ChartMetric
import com.kova.app.domain.model.ExerciseAnalytics
import com.kova.app.domain.model.ExerciseSessionPoint
import com.kova.app.domain.model.ProgressTrend
import com.kova.app.domain.model.WeightUnit
import com.kova.app.domain.model.WorkoutSession
import com.kova.app.domain.model.WorkoutSet
import kotlin.math.abs

object ProgressAnalyticsEngine {

    private const val REFERENCE_BODYWEIGHT_KG = 70.0
    private const val PLATEAU_TOLERANCE_RATIO = 0.005 // 0.5%

    /**
     * Epley estimated 1RM formula:
     * reps == 1 -> weightKg
     * reps > 1  -> weightKg * (1 + reps / 30.0)
     */
    fun estimate1RmKg(weightKg: Double, reps: Int): Double {
        if (reps <= 0 || weightKg <= 0.0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps / 30.0)
    }

    /**
     * Internal progression score that smoothly compares pure bodyweight and weighted bodyweight sets
     * as well as standard loaded exercises.
     */
    fun progressionScore(weightKg: Double, reps: Int, isBodyweight: Boolean): Double {
        if (reps <= 0) return 0.0
        return if (isBodyweight) {
            val effectiveLoad = REFERENCE_BODYWEIGHT_KG + weightKg.coerceAtLeast(0.0)
            if (reps == 1) effectiveLoad else effectiveLoad * (1.0 + reps / 30.0)
        } else {
            estimate1RmKg(weightKg, reps)
        }
    }

    fun analyzeExercise(
        sessionsWithSets: List<Pair<WorkoutSession, List<WorkoutSet>>>,
        isBodyweightExercise: Boolean,
        unit: WeightUnit = WeightUnit.KG
    ): ExerciseAnalytics {
        val chronological = sessionsWithSets
            .mapNotNull { (session, sets) ->
                val valid = sets.filter { it.isCompleted && it.reps > 0 }.sortedBy { it.setIndex }
                if (valid.isEmpty()) return@mapNotNull null

                val allZeroLoad = valid.all { it.weightKg <= 0.0 }
                val isPureBw = isBodyweightExercise && allZeroLoad

                val maxWeight = valid.maxOf { it.weightKg }
                val bestReps = valid.maxOf { it.reps }
                val totalReps = valid.sumOf { it.reps }
                val totalVolume = valid.sumOf { it.weightKg * it.reps }
                val best1Rm = valid.maxOf { estimate1RmKg(it.weightKg, it.reps) }
                val bestScore = valid.maxOf { progressionScore(it.weightKg, it.reps, isBodyweightExercise || it.isBodyweightSnapshot) }

                val bestSet = valid.maxBy {
                    progressionScore(it.weightKg, it.reps, isBodyweightExercise || it.isBodyweightSnapshot)
                }
                val bestDesc = "${WeightFormatter.formatWeight(bestSet.weightKg, unit, isBodyweightExercise || bestSet.isBodyweightSnapshot)} × ${bestSet.reps}"

                ExerciseSessionPoint(
                    sessionId = session.id,
                    date = session.date,
                    sets = valid,
                    estimated1RmKg = best1Rm,
                    maxWeightKg = maxWeight,
                    totalVolumeKg = totalVolume,
                    bestReps = bestReps,
                    totalReps = totalReps,
                    isPureBodyweight = isPureBw,
                    bestSetDescription = bestDesc,
                    progressionScore = bestScore
                )
            }
            .sortedWith(compareBy<ExerciseSessionPoint> { it.date }.thenBy { it.sessionId })

        if (chronological.isEmpty()) {
            return ExerciseAnalytics(
                points = emptyList(),
                isBodyweightExercise = isBodyweightExercise,
                isPureBodyweightSeries = isBodyweightExercise,
                allTimeBest1RmKg = 0.0,
                allTimeMaxWeightKg = 0.0,
                allTimeBestVolumeKg = 0.0,
                allTimeBestReps = 0,
                trend = ProgressTrend.BUILDING_HISTORY,
                trendDeltaPercent = null,
                plateauSessionCount = 0,
                summaryMessage = "Aún no hay registros. Completa tu primera sesión para ver tu gráfica.",
                prPointIndexByMetric = emptyMap()
            )
        }

        val isPureSeries = isBodyweightExercise && chronological.all { it.isPureBodyweight }
        val allTimeBest1Rm = chronological.maxOf { it.estimated1RmKg }
        val allTimeMaxWeight = chronological.maxOf { it.maxWeightKg }
        val allTimeBestVolume = chronological.maxOf { it.totalVolumeKg }
        val allTimeBestReps = chronological.maxOf { it.bestReps }

        val prIndices = ChartMetric.entries.associateWith { metric ->
            var bestIdx = 0
            var bestVal = Double.NEGATIVE_INFINITY
            chronological.forEachIndexed { index, point ->
                val v = point.valueFor(metric, isPureSeries)
                if (v >= bestVal) {
                    bestVal = v
                    bestIdx = index
                }
            }
            bestIdx
        }

        if (chronological.size == 1) {
            return ExerciseAnalytics(
                points = chronological,
                isBodyweightExercise = isBodyweightExercise,
                isPureBodyweightSeries = isPureSeries,
                allTimeBest1RmKg = allTimeBest1Rm,
                allTimeMaxWeightKg = allTimeMaxWeight,
                allTimeBestVolumeKg = allTimeBestVolume,
                allTimeBestReps = allTimeBestReps,
                trend = ProgressTrend.BUILDING_HISTORY,
                trendDeltaPercent = null,
                plateauSessionCount = 0,
                summaryMessage = "Primer registro guardado. En la próxima sesión empezarás a ver tu tendencia.",
                prPointIndexByMetric = prIndices
            )
        }

        val latest = chronological.last()
        val previousPoints = chronological.dropLast(1)
        val prevBestScore = previousPoints.maxOf { it.progressionScore }
        val prevScore = previousPoints.last().progressionScore
        val deltaVsPrevious = if (prevScore > 0.0) {
            ((latest.progressionScore - prevScore) / prevScore) * 100.0
        } else 0.0

        // Check if strictly beating all previous sessions on progression score (with tiny float epsilon)
        val isNewPr = latest.progressionScore > prevBestScore * (1.0 + 0.001)

        // Check plateau: count trailing sessions where progressionScore hasn't improved beyond tolerance
        val plateauCount = countTrailingPlateauSessions(chronological)

        val (trend, message) = when {
            isNewPr -> {
                val gainPct = if (prevBestScore > 0.0) {
                    ((latest.progressionScore - prevBestScore) / prevBestScore) * 100.0
                } else 0.0
                val rounded = String.format("%.1f", gainPct)
                ProgressTrend.NEW_PR to "¡Mejor marca histórica! Subiste un +$rounded% sobre tu récord anterior."
            }
            plateauCount >= 3 -> {
                ProgressTrend.PLATEAU to "$plateauCount sesiones seguidas sin superar tu techo. Prueba subir +${unit.stepLabel} o buscar +1 rep en la primera serie."
            }
            deltaVsPrevious >= 0.5 -> {
                val rounded = String.format("%.1f", deltaVsPrevious)
                ProgressTrend.IMPROVING to "Buena progresión (+$rounded% respecto a la sesión anterior)."
            }
            deltaVsPrevious <= -3.0 -> {
                val rounded = String.format("%.1f", abs(deltaVsPrevious))
                ProgressTrend.DIP to "Rendimiento un -$rounded% respecto a la última sesión. Normal si hubo fatiga acumulada."
            }
            else -> {
                ProgressTrend.IMPROVING to "Rendimiento estable respecto a tu última sesión."
            }
        }

        return ExerciseAnalytics(
            points = chronological,
            isBodyweightExercise = isBodyweightExercise,
            isPureBodyweightSeries = isPureSeries,
            allTimeBest1RmKg = allTimeBest1Rm,
            allTimeMaxWeightKg = allTimeMaxWeight,
            allTimeBestVolumeKg = allTimeBestVolume,
            allTimeBestReps = allTimeBestReps,
            trend = trend,
            trendDeltaPercent = deltaVsPrevious,
            plateauSessionCount = plateauCount,
            summaryMessage = message,
            prPointIndexByMetric = prIndices
        )
    }

    /**
     * Counts how many consecutive sessions ending at the latest have failed to beat
     * the anchor session (no progression beyond tolerance on score, weight, or best reps).
     */
    private fun countTrailingPlateauSessions(points: List<ExerciseSessionPoint>): Int {
        if (points.size < 3) return 0
        val latest = points.last()
        var count = 1
        for (i in points.lastIndex - 1 downTo 0) {
            val candidate = points[i]
            val maxScore = maxOf(candidate.progressionScore, latest.progressionScore)
            val scoreDiffRatio = if (maxScore > 0.0) abs(latest.progressionScore - candidate.progressionScore) / maxScore else 0.0
            val sameOrLower = latest.progressionScore <= candidate.progressionScore * (1.0 + PLATEAU_TOLERANCE_RATIO)
            val closeBand = scoreDiffRatio <= 0.035 // within ~3.5% ceiling band
            if (sameOrLower && closeBand) {
                count++
            } else {
                break
            }
        }
        return count
    }

    /**
     * Checks if a newly completed set beats all previous historical sets for this exercise.
     */
    fun isSetNewPr(
        weightKg: Double,
        reps: Int,
        isBodyweight: Boolean,
        historicalSets: List<WorkoutSet>
    ): Boolean {
        if (reps <= 0) return false
        val validHistory = historicalSets.filter { it.isCompleted && it.reps > 0 }
        if (validHistory.isEmpty()) return false
        val candidateScore = progressionScore(weightKg, reps, isBodyweight)
        val bestHistoricalScore = validHistory.maxOf {
            progressionScore(it.weightKg, it.reps, isBodyweight || it.isBodyweightSnapshot)
        }
        return candidateScore > bestHistoricalScore * 1.001
    }
}
