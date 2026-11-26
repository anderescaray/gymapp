package com.kova.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kova.app.domain.logic.WeightFormatter
import com.kova.app.domain.model.ChartMetric
import com.kova.app.domain.model.Exercise
import com.kova.app.domain.model.ExerciseAnalytics
import com.kova.app.domain.model.ProgressTrend
import com.kova.app.domain.model.RoutineDay
import com.kova.app.domain.model.WeightUnit
import com.kova.app.ui.components.ExerciseEditorSheet
import com.kova.app.ui.components.InteractiveProgressChart
import com.kova.app.ui.components.KovaCard
import com.kova.app.ui.components.KovaPrimaryButton
import com.kova.app.ui.components.KovaSecondaryButton
import com.kova.app.ui.components.KovaSectionHeader
import com.kova.app.ui.components.KovaSegmentedControl
import com.kova.app.ui.components.KovaStatusPill
import com.kova.app.ui.components.parseHexColor
import com.kova.app.ui.theme.KovaTheme
import com.kova.app.ui.theme.tabular
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val historyRowDateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.forLanguageTag("es-ES"))
private val manualRmDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-ES"))

@Composable
fun ExerciseDetailScreen(
    exercise: Exercise?,
    routineDay: RoutineDay?,
    analytics: ExerciseAnalytics,
    unit: WeightUnit,
    defaultRestSeconds: Int,
    onBack: () -> Unit,
    onUpdateExercise: (Exercise) -> Unit,
    onUpdateManualOneRm: (exerciseId: Long, oneRmKg: Double?) -> Unit
) {
    val colors = KovaTheme.colors
    if (exercise == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.canvas),
            contentAlignment = Alignment.Center
        ) {
            KovaSecondaryButton(text = "Volver", onClick = onBack)
        }
        return
    }

    var selectedMetric by remember {
        mutableStateOf(
            if (analytics.isPureBodyweightSeries) ChartMetric.BEST_REPS else ChartMetric.ESTIMATED_1RM
        )
    }
    var showEditExerciseSheet by remember { mutableStateOf(false) }
    var showManualOneRmSheet by remember { mutableStateOf(false) }

    val reversedHistory = remember(analytics.points) { analytics.points.reversed() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver",
                            tint = colors.inkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.inkPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (routineDay != null) {
                                KovaStatusPill(
                                    text = routineDay.name,
                                    containerColor = colors.surfaceTonal,
                                    contentColor = colors.inkPrimary,
                                    leadingDotColor = parseHexColor(routineDay.colorHex)
                                )
                            }
                            KovaStatusPill(
                                text = if (exercise.isMandatory) "Obligatorio" else "Opcional",
                                containerColor = if (exercise.isMandatory) colors.accentSoft else colors.surfaceTonal,
                                contentColor = if (exercise.isMandatory) colors.accentText else colors.inkSecondary
                            )
                            KovaStatusPill(
                                text = "${exercise.defaultRestSeconds ?: defaultRestSeconds}s",
                                containerColor = colors.surfaceTonal,
                                contentColor = colors.inkSecondary
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .clickable { showEditExerciseSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Editar ejercicio",
                        tint = colors.inkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Trend & Plateau Coaching Card
        item {
            val (bg, border, fg, icon) = when (analytics.trend) {
                ProgressTrend.NEW_PR -> QuadColor(colors.successSoft, colors.successBorder, colors.success, Icons.Rounded.EmojiEvents)
                ProgressTrend.IMPROVING -> QuadColor(colors.accentSoft, colors.border, colors.accentText, Icons.AutoMirrored.Rounded.TrendingUp)
                ProgressTrend.PLATEAU -> QuadColor(colors.warningSoft, colors.warningBorder, colors.warning, Icons.Rounded.HorizontalRule)
                ProgressTrend.DIP -> QuadColor(colors.surface, colors.border, colors.inkSecondary, Icons.AutoMirrored.Rounded.TrendingDown)
                ProgressTrend.BUILDING_HISTORY -> QuadColor(colors.surface, colors.border, colors.inkSecondary, Icons.AutoMirrored.Rounded.TrendingUp)
            }

            KovaCard(
                containerColor = bg,
                borderColor = border,
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.surface.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = fg,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = analytics.trend.badgeLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = fg
                        )
                        Text(
                            text = analytics.summaryMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkPrimary
                        )
                    }
                }
            }
        }

        // Real Manual 1RM Spotlight Card (always prominent when recorded; compact add button when unset)
        item {
            RealOneRmCard(
                manualOneRmKg = exercise.manualOneRmKg,
                manualOneRmDateEpochDay = exercise.manualOneRmDateEpochDay,
                estimatedOneRmKg = analytics.allTimeBest1RmKg.takeIf { !analytics.isPureBodyweightSeries && it > 0.0 },
                unit = unit,
                onClick = { showManualOneRmSheet = true }
            )
        }

        // Interactive Progress Chart Card
        item {
            KovaCard(contentPadding = PaddingValues(18.dp)) {
                KovaSegmentedControl(
                    options = ChartMetric.entries,
                    selectedOption = selectedMetric,
                    labelProvider = { it.shortLabel },
                    onSelect = { selectedMetric = it }
                )
                Spacer(Modifier.height(16.dp))
                InteractiveProgressChart(
                    points = analytics.points,
                    metric = selectedMetric,
                    prIndex = analytics.prPointIndexByMetric[selectedMetric],
                    unit = unit,
                    isBodyweightExercise = analytics.isBodyweightExercise,
                    isPureBodyweightSeries = analytics.isPureBodyweightSeries
                )
            }
        }

        // All-Time Records Grid
        if (analytics.points.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RecordTile(
                        label = if (analytics.isPureBodyweightSeries) "Mejor serie" else "1RM estimado",
                        value = if (analytics.isPureBodyweightSeries) {
                            "${analytics.allTimeBestReps} reps"
                        } else {
                            "${WeightFormatter.formatNumber(WeightFormatter.kgToDisplay(analytics.allTimeBest1RmKg, unit))} ${unit.label}"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    RecordTile(
                        label = "Peso máx.",
                        value = WeightFormatter.formatWeight(analytics.allTimeMaxWeightKg, unit, false),
                        modifier = Modifier.weight(1f)
                    )
                    RecordTile(
                        label = "Sesiones",
                        value = "${analytics.points.size}",
                        modifier = Modifier.weight(0.78f)
                    )
                }
            }
        }

        // Historical Sessions Timeline
        if (reversedHistory.isNotEmpty()) {
            item {
                KovaSectionHeader(title = "Historial de sesiones (${reversedHistory.size})")
            }
            val prIdxOnPrimary = analytics.prPointIndexByMetric[ChartMetric.ESTIMATED_1RM]
            itemsIndexed(reversedHistory, key = { _, pt -> pt.sessionId }) { revIdx, point ->
                val chronologicalIndex = analytics.points.lastIndex - revIdx
                val isPrSession = prIdxOnPrimary == chronologicalIndex && analytics.points.size >= 2
                KovaCard(contentPadding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = point.date.format(historyRowDateFormatter).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.inkPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isPrSession) {
                                KovaStatusPill(
                                    text = "PR",
                                    containerColor = colors.successSoft,
                                    contentColor = colors.success
                                )
                            }
                            Text(
                                text = if (point.isPureBodyweight) "${point.totalReps} reps" else WeightFormatter.formatVolume(point.totalVolumeKg, unit),
                                style = MaterialTheme.typography.labelMedium.tabular(),
                                color = colors.inkSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        point.sets.forEach { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceTonal)
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Serie ${s.setIndex}",
                                    style = MaterialTheme.typography.bodySmall.tabular(),
                                    color = colors.inkTertiary
                                )
                                Text(
                                    text = "${WeightFormatter.formatWeight(s.weightKg, unit, false)} × ${s.reps} reps",
                                    style = MaterialTheme.typography.titleSmall.tabular().copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.inkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditExerciseSheet) {
        ExerciseEditorSheet(
            existing = exercise,
            defaultRestSeconds = defaultRestSeconds,
            onDismiss = { showEditExerciseSheet = false },
            onSave = { name, mandatory, rest ->
                onUpdateExercise(
                    exercise.copy(
                        name = name,
                        isMandatory = mandatory,
                        isBodyweight = false,
                        notes = "",
                        defaultRestSeconds = rest
                    )
                )
                showEditExerciseSheet = false
            }
        )
    }

    if (showManualOneRmSheet) {
        ManualOneRmEditorSheet(
            exerciseName = exercise.name,
            currentOneRmKg = exercise.manualOneRmKg,
            estimatedOneRmKg = analytics.allTimeBest1RmKg.takeIf { it > 0.0 },
            unit = unit,
            onDismiss = { showManualOneRmSheet = false },
            onSave = { kg ->
                onUpdateManualOneRm(exercise.id, kg)
                showManualOneRmSheet = false
            },
            onClear = {
                onUpdateManualOneRm(exercise.id, null)
                showManualOneRmSheet = false
            }
        )
    }
}

@Composable
private fun RealOneRmCard(
    manualOneRmKg: Double?,
    manualOneRmDateEpochDay: Long?,
    estimatedOneRmKg: Double?,
    unit: WeightUnit,
    onClick: () -> Unit
) {
    val colors = KovaTheme.colors
    if (manualOneRmKg == null) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.accentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = colors.accentText,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "1RM real",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.inkPrimary
                        )
                        val estText = estimatedOneRmKg?.let {
                            "Estimada actual: ${WeightFormatter.formatNumber(WeightFormatter.kgToDisplay(it, unit))} ${unit.label}"
                        } ?: "Anota tu marca máxima real"
                        Text(
                            text = estText,
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = colors.inkSecondary
                        )
                    }
                }
                KovaStatusPill(
                    text = "+ Anotar",
                    containerColor = colors.accentSoft,
                    contentColor = colors.accentText
                )
            }
        }
        return
    }

    val highlight = if (colors.isDark) colors.accent else colors.accentText
    val gradientBrush = if (colors.isDark) {
        Brush.linearGradient(
            colors = listOf(
                colors.accent.copy(alpha = 0.28f),
                colors.accentSoft,
                colors.surface
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                colors.accentSoft,
                colors.surface
            )
        )
    }
    val formattedReal = WeightFormatter.formatNumber(WeightFormatter.kgToDisplay(manualOneRmKg, unit))
    val dateText = manualOneRmDateEpochDay?.let {
        LocalDate.ofEpochDay(it).format(manualRmDateFormatter)
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        border = BorderStroke(2.dp, highlight.copy(alpha = if (colors.isDark) 0.92f else 0.75f)),
        shadowElevation = if (colors.isDark) 10.dp else 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = colors.accent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = colors.onAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "1RM REAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            ),
                            color = colors.onAccent
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Editar 1RM real",
                        tint = highlight,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Editar",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = highlight
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formattedReal,
                        style = MaterialTheme.typography.displayLarge.tabular().copy(
                            fontSize = 42.sp,
                            lineHeight = 44.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = if (colors.isDark) colors.accent else colors.inkPrimary
                    )
                    Text(
                        text = unit.label,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = highlight,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (estimatedOneRmKg != null) {
                        Text(
                            text = "Estimada: ${WeightFormatter.formatNumber(WeightFormatter.kgToDisplay(estimatedOneRmKg, unit))} ${unit.label}",
                            style = MaterialTheme.typography.labelMedium.tabular(),
                            color = colors.inkSecondary
                        )
                    }
                    if (dateText != null) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = colors.inkTertiary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualOneRmEditorSheet(
    exerciseName: String,
    currentOneRmKg: Double?,
    estimatedOneRmKg: Double?,
    unit: WeightUnit,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onClear: () -> Unit
) {
    val colors = KovaTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialDisplay = currentOneRmKg?.let { WeightFormatter.formatNumber(WeightFormatter.kgToDisplay(it, unit)) }.orEmpty()
    val placeholderDisplay = estimatedOneRmKg?.let { WeightFormatter.formatNumber(WeightFormatter.kgToDisplay(it, unit)) } ?: "60"
    var weightText by remember(currentOneRmKg, unit) { mutableStateOf(initialDisplay) }

    val parsedDisplay = weightText.toDoubleOrNull()
    val canSave = parsedDisplay != null && parsedDisplay > 0.0 && parsedDisplay <= 2000.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.inkPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "1RM real · $exerciseName",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.inkPrimary
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = weightText,
                onValueChange = { raw ->
                    val clean = raw.replace(',', '.').filter { it.isDigit() || it == '.' }.take(6)
                    if (clean.count { it == '.' } <= 1) {
                        val v = clean.toDoubleOrNull()
                        if (v == null || v <= 2000.0) {
                            weightText = clean
                        }
                    }
                },
                label = { Text("Peso (${unit.label})") },
                placeholder = { Text("Ej. $placeholderDisplay") },
                suffix = { Text(unit.label, color = colors.inkSecondary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border,
                    focusedLabelColor = colors.accentText
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(22.dp))

            KovaPrimaryButton(
                text = "Guardar 1RM real",
                icon = Icons.Rounded.Add,
                enabled = canSave,
                onClick = {
                    val kg = WeightFormatter.displayToKg(parsedDisplay ?: return@KovaPrimaryButton, unit)
                    onSave(kg)
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (currentOneRmKg != null) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = onClear) {
                        Text(
                            text = "Quitar 1RM real",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.danger
                        )
                    }
                }
            }
        }
    }
}

private data class QuadColor(
    val bg: Color,
    val border: Color,
    val fg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun RecordTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = KovaTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                color = colors.inkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
