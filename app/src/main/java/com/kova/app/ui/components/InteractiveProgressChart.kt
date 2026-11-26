package com.kova.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kova.app.domain.logic.WeightFormatter
import com.kova.app.domain.model.ChartMetric
import com.kova.app.domain.model.ExerciseSessionPoint
import com.kova.app.domain.model.WeightUnit
import com.kova.app.ui.theme.KovaTheme
import com.kova.app.ui.theme.tabular
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-ES"))
private val fullDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-ES"))

@Composable
fun InteractiveProgressChart(
    points: List<ExerciseSessionPoint>,
    metric: ChartMetric,
    prIndex: Int?,
    unit: WeightUnit,
    isBodyweightExercise: Boolean,
    isPureBodyweightSeries: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = KovaTheme.colors
    val haptics = LocalHapticFeedback.current

    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Completa sesiones para ver tu evolución aquí",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkTertiary
            )
        }
        return
    }

    var selectedIndex by remember(points.size) {
        mutableIntStateOf(points.lastIndex)
    }

    LaunchedEffect(points.size, metric) {
        selectedIndex = points.lastIndex
    }

    val safeSelected = selectedIndex.coerceIn(0, points.lastIndex)
    val selectedPoint = points[safeSelected]
    val selectedValueText = formatChartValue(
        rawValue = selectedPoint.valueFor(metric, isPureBodyweightSeries),
        metric = metric,
        unit = unit,
        isBodyweightExercise = isBodyweightExercise,
        isPureBodyweightSeries = isPureBodyweightSeries
    )
    val isSelectedPr = prIndex == safeSelected && points.size >= 2

    val lineColor = if (colors.isDark) colors.accent else colors.accentText
    val prColor = colors.success
    val gridColor = colors.border

    Column(modifier = modifier.fillMaxWidth()) {
        // Live Scrubbing Readout Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = selectedValueText,
                        style = MaterialTheme.typography.headlineMedium.tabular().copy(fontWeight = FontWeight.Bold),
                        color = colors.inkPrimary
                    )
                    if (isSelectedPr) {
                        KovaStatusPill(
                            text = "PR",
                            containerColor = colors.successSoft,
                            contentColor = colors.success,
                            borderColor = colors.successBorder
                        )
                    }
                }
                Text(
                    text = "${selectedPoint.date.format(fullDateFormatter)} · Mejor serie: ${selectedPoint.bestSetDescription}",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = colors.inkSecondary
                )
            }
            Text(
                text = "${selectedPoint.sets.size} ${if (selectedPoint.sets.size == 1) "serie" else "series"}",
                style = MaterialTheme.typography.labelMedium.tabular(),
                color = colors.inkTertiary
            )
        }

        Spacer(Modifier.height(14.dp))

        val values = remember(points, metric, isPureBodyweightSeries) {
            points.map { it.valueFor(metric, isPureBodyweightSeries) }
        }
        val minVal = values.minOrNull() ?: 0.0
        val maxVal = values.maxOrNull() ?: 1.0
        val spread = (maxVal - minVal).let { if (it < 0.001) maxOf(1.0, maxVal * 0.2) else it }
        val yMin = (minVal - spread * 0.18).coerceAtLeast(0.0)
        val yMax = maxVal + spread * 0.18
        val yRange = (yMax - yMin).coerceAtLeast(1.0)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(195.dp)
        ) {
            // Subtle Y-axis labels on left edge
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(top = 6.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatCompactAxisValue(yMax, metric, unit, isBodyweightExercise, isPureBodyweightSeries),
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = colors.inkTertiary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatCompactAxisValue((yMax + yMin) / 2.0, metric, unit, isBodyweightExercise, isPureBodyweightSeries),
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = colors.inkTertiary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatCompactAxisValue(yMin, metric, unit, isBodyweightExercise, isPureBodyweightSeries),
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = colors.inkTertiary
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 42.dp, end = 10.dp, top = 12.dp, bottom = 26.dp)
                    .pointerInput(points.size, metric) {
                        detectTapGestures { offset ->
                            val idx = findNearestIndex(offset.x, size.width.toFloat(), points.size)
                            if (idx != selectedIndex) {
                                selectedIndex = idx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                    .pointerInput(points.size, metric) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val idx = findNearestIndex(change.position.x, size.width.toFloat(), points.size)
                            if (idx != selectedIndex) {
                                selectedIndex = idx
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                // Horizontal dashed gridlines
                listOf(0f, 0.5f, 1f).forEach { frac ->
                    val y = h * frac
                    drawLine(
                        color = gridColor.copy(alpha = 0.7f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                val coords = values.mapIndexed { idx, v ->
                    val x = if (points.size == 1) {
                        w * 0.5f
                    } else {
                        (idx.toFloat() / points.lastIndex.toFloat()) * w
                    }
                    val normY = ((v - yMin) / yRange).toFloat().coerceIn(0f, 1f)
                    val y = h - (normY * h)
                    Offset(x, y)
                }

                // Vertical cursor hairline at selected point
                val selCoord = coords[safeSelected]
                drawLine(
                    color = lineColor.copy(alpha = 0.35f),
                    start = Offset(selCoord.x, 0f),
                    end = Offset(selCoord.x, h),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = dashEffect
                )

                if (coords.size >= 2) {
                    val strokePath = buildSmoothBezierPath(coords)
                    val fillPath = Path().apply {
                        addPath(strokePath)
                        lineTo(coords.last().x, h)
                        lineTo(coords.first().x, h)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = if (colors.isDark) 0.24f else 0.16f),
                                lineColor.copy(alpha = 0.0f)
                            ),
                            startY = 0f,
                            endY = h
                        )
                    )

                    drawPath(
                        path = strokePath,
                        color = lineColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Data dots
                coords.forEachIndexed { idx, pt ->
                    val isPr = prIndex == idx && points.size >= 2
                    val isSel = idx == safeSelected
                    when {
                        isSel -> {
                            drawCircle(
                                color = (if (isPr) prColor else lineColor).copy(alpha = 0.22f),
                                radius = 11.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = colors.surface,
                                radius = 6.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = if (isPr) prColor else lineColor,
                                radius = 4.2.dp.toPx(),
                                center = pt
                            )
                        }
                        isPr -> {
                            drawCircle(
                                color = prColor.copy(alpha = 0.20f),
                                radius = 7.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = prColor,
                                radius = 3.6.dp.toPx(),
                                center = pt
                            )
                        }
                        else -> {
                            drawCircle(
                                color = lineColor,
                                radius = 2.8.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            }

            // Bottom X-axis date labels
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 42.dp, end = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = points.first().date.format(shortDateFormatter),
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = colors.inkTertiary
                )
                if (points.size >= 3) {
                    Text(
                        text = points[points.size / 2].date.format(shortDateFormatter),
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.inkTertiary
                    )
                }
                if (points.size >= 2) {
                    Text(
                        text = points.last().date.format(shortDateFormatter),
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.inkTertiary
                    )
                }
            }
        }
    }
}

private fun findNearestIndex(touchX: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0f) return 0
    var bestIdx = 0
    var bestDist = Float.MAX_VALUE
    for (i in 0 until count) {
        val x = (i.toFloat() / (count - 1).toFloat()) * width
        val d = abs(touchX - x)
        if (d < bestDist) {
            bestDist = d
            bestIdx = i
        }
    }
    return bestIdx
}

private fun buildSmoothBezierPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (i in 0 until points.lastIndex) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val controlX = (p0.x + p1.x) / 2f
        path.cubicTo(
            controlX, p0.y,
            controlX, p1.y,
            p1.x, p1.y
        )
    }
    return path
}

private fun formatChartValue(
    rawValue: Double,
    metric: ChartMetric,
    unit: WeightUnit,
    isBodyweightExercise: Boolean,
    isPureBodyweightSeries: Boolean
): String = when (metric) {
    ChartMetric.ESTIMATED_1RM -> when {
        isPureBodyweightSeries -> "${rawValue.toInt()} reps"
        isBodyweightExercise -> "${WeightFormatter.formatNumber(WeightFormatter.kgToDisplay(rawValue, unit))} ${unit.label} (est.)"
        else -> "${WeightFormatter.formatNumber(WeightFormatter.kgToDisplay(rawValue, unit))} ${unit.label}"
    }
    ChartMetric.MAX_WEIGHT -> {
        WeightFormatter.formatWeight(rawValue, unit, isBodyweightExercise)
    }
    ChartMetric.TOTAL_VOLUME -> {
        if (isPureBodyweightSeries) "${rawValue.toInt()} reps tot."
        else if (isBodyweightExercise && rawValue <= 0.01) "PC"
        else WeightFormatter.formatVolume(rawValue, unit)
    }
    ChartMetric.BEST_REPS -> "${rawValue.toInt()} reps"
}

private fun formatCompactAxisValue(
    rawValue: Double,
    metric: ChartMetric,
    unit: WeightUnit,
    isBodyweightExercise: Boolean,
    isPureBodyweightSeries: Boolean
): String = when (metric) {
    ChartMetric.ESTIMATED_1RM -> {
        if (isPureBodyweightSeries) "${rawValue.toInt()}"
        else WeightFormatter.formatNumber(WeightFormatter.kgToDisplay(rawValue, unit))
    }
    ChartMetric.MAX_WEIGHT -> {
        val num = WeightFormatter.formatNumber(WeightFormatter.kgToDisplay(rawValue, unit))
        if (isBodyweightExercise && rawValue > 0.01) "+$num" else num
    }
    ChartMetric.TOTAL_VOLUME -> {
        if (isPureBodyweightSeries) {
            "${rawValue.toInt()}"
        } else {
            val disp = WeightFormatter.kgToDisplay(rawValue, unit)
            if (disp >= 1000.0) String.format(Locale.US, "%.1fk", disp / 1000.0)
            else "${disp.toInt()}"
        }
    }
    ChartMetric.BEST_REPS -> "${rawValue.toInt()}"
}
