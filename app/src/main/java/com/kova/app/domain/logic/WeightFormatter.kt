package com.kova.app.domain.logic

import com.kova.app.domain.model.WeightUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object WeightFormatter {
    const val LB_PER_KG = 2.20462262185

    fun kgToDisplay(kg: Double, unit: WeightUnit): Double {
        return when (unit) {
            WeightUnit.KG -> kg
            WeightUnit.LB -> (kg * LB_PER_KG * 2.0).roundToInt() / 2.0
        }
    }

    fun displayToKg(displayValue: Double, unit: WeightUnit): Double {
        val clamped = displayValue.coerceAtLeast(0.0)
        return when (unit) {
            WeightUnit.KG -> clamped
            WeightUnit.LB -> clamped / LB_PER_KG
        }
    }

    fun stepWeightKg(currentKg: Double, deltaSign: Int, unit: WeightUnit): Double {
        return when (unit) {
            WeightUnit.KG -> {
                val next = currentKg + (deltaSign * 2.5)
                ((next.coerceAtLeast(0.0) * 4.0).roundToInt() / 4.0)
            }
            WeightUnit.LB -> {
                val currentLb = kgToDisplay(currentKg, WeightUnit.LB)
                val nextLb = (currentLb + (deltaSign * 5.0)).coerceAtLeast(0.0)
                displayToKg(nextLb, WeightUnit.LB)
            }
        }
    }

    fun formatNumber(value: Double): String {
        val rounded = (value * 100.0).roundToInt() / 100.0
        return if (abs(rounded - rounded.roundToInt()) < 0.01) {
            rounded.roundToInt().toString()
        } else if (abs(rounded * 10.0 - (rounded * 10.0).roundToInt()) < 0.05) {
            String.format(Locale.US, "%.1f", rounded)
        } else {
            String.format(Locale.US, "%.2f", rounded)
        }
    }

    fun formatWeight(kg: Double, unit: WeightUnit, isBodyweight: Boolean = false): String {
        val display = kgToDisplay(kg, unit)
        return if (isBodyweight) {
            if (display <= 0.01) {
                "PC"
            } else {
                "PC +${formatNumber(display)} ${unit.label}"
            }
        } else {
            "${formatNumber(display)} ${unit.label}"
        }
    }

    fun formatCompactWeight(kg: Double, unit: WeightUnit, isBodyweight: Boolean = false): String {
        val display = kgToDisplay(kg, unit)
        return if (isBodyweight) {
            if (display <= 0.01) "PC" else "+${formatNumber(display)}"
        } else {
            formatNumber(display)
        }
    }

    fun formatVolume(volumeKg: Double, unit: WeightUnit): String {
        val display = kgToDisplay(volumeKg, unit)
        val rounded = display.roundToInt()
        val formatted = String.format(Locale.forLanguageTag("es-ES"), "%,d", rounded)
        return "$formatted ${unit.label}"
    }

    fun formatDuration(elapsedSeconds: Long): String {
        val safe = elapsedSeconds.coerceAtLeast(0L)
        val hours = safe / 3600
        val mins = (safe % 3600) / 60
        val secs = safe % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }
}
