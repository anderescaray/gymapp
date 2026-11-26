package com.kova.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kova.app.domain.logic.CalendarEvaluator
import com.kova.app.domain.logic.WeightFormatter
import com.kova.app.domain.model.CalendarDayCell
import com.kova.app.domain.model.CalendarWeekSummary
import com.kova.app.domain.model.WeekStatus
import com.kova.app.domain.model.WeightUnit
import com.kova.app.domain.model.WorkoutSession
import com.kova.app.domain.model.WorkoutSet
import com.kova.app.ui.components.KovaCard
import com.kova.app.ui.components.KovaStatusPill
import com.kova.app.ui.components.KovaStepper
import com.kova.app.ui.components.parseHexColor
import com.kova.app.ui.state.KovaUiState
import com.kova.app.ui.theme.KovaTheme
import com.kova.app.ui.theme.tabular
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthHeaderFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-ES"))
private val dayDetailFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-ES"))

@Composable
fun CalendarScreen(
    uiState: KovaUiState,
    setsForSessionProvider: (Long) -> List<WorkoutSet>,
    onUpdateWeeklyGoal: (Int) -> Unit,
    onDeleteSession: (Long) -> Unit
) {
    val colors = KovaTheme.colors
    val today = LocalDate.now()
    var displayedMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val trackingStart = remember(uiState.preferences.trackingStartEpochDay) {
        LocalDate.ofEpochDay(uiState.preferences.trackingStartEpochDay)
    }
    val finishedSessions = remember(uiState.allSessions) {
        uiState.allSessions.filter { !it.isActive }
    }
    val weeks = remember(displayedMonth, finishedSessions, uiState.preferences.weeklyGoalDays, trackingStart, today) {
        CalendarEvaluator.buildMonthWeeks(
            month = displayedMonth,
            finishedSessions = finishedSessions,
            weeklyGoal = uiState.preferences.weeklyGoalDays,
            trackingStartDate = trackingStart,
            today = today
        )
    }
    val monthWorkouts = remember(displayedMonth, finishedSessions) {
        CalendarEvaluator.countWorkoutsInMonth(displayedMonth, finishedSessions)
    }
    // Count ISO weeks whose majority (Thursday) belongs to displayedMonth so adjacent months don't double-count
    val primaryWeeksInMonth = remember(weeks, displayedMonth) {
        weeks.filter { YearMonth.from(it.weekStart.plusDays(3)) == displayedMonth }
    }
    val metWeeksInMonth = remember(primaryWeeksInMonth) { primaryWeeksInMonth.count { it.status == WeekStatus.MET } }
    val missedWeeksInMonth = remember(primaryWeeksInMonth) { primaryWeeksInMonth.count { it.status == WeekStatus.MISSED } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header + Streak Pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONSTANCIA",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inkSecondary
                    )
                    Text(
                        text = "Calendario",
                        style = MaterialTheme.typography.displayLarge,
                        color = colors.inkPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (uiState.weeklyStreak > 0) colors.warning else colors.inkTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${uiState.weeklyStreak} sem.",
                            style = MaterialTheme.typography.labelLarge.tabular(),
                            color = colors.inkPrimary
                        )
                    }
                }
            }
        }

        // Month KPI Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CalendarStatChip(
                    value = "$monthWorkouts",
                    label = "Días este mes",
                    tint = colors.inkPrimary,
                    modifier = Modifier.weight(1f)
                )
                CalendarStatChip(
                    value = "$metWeeksInMonth",
                    label = "Sem. cumplidas",
                    tint = colors.success,
                    modifier = Modifier.weight(1f)
                )
                CalendarStatChip(
                    value = "$missedWeeksInMonth",
                    label = "Sin objetivo",
                    tint = if (missedWeeksInMonth > 0) colors.danger else colors.inkTertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Main Calendar Card with Green / Red Weekly Bands
        item {
            KovaCard(contentPadding = PaddingValues(16.dp)) {
                // Month Navigation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceTonal)
                            .clickable { displayedMonth = displayedMonth.minusMonths(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Mes anterior",
                            tint = colors.inkPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = displayedMonth.format(monthHeaderFormatter).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.inkPrimary
                        )
                        if (displayedMonth != YearMonth.from(today)) {
                            Text(
                                text = "Volver a hoy",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accentText,
                                modifier = Modifier
                                    .clickable { displayedMonth = YearMonth.from(today) }
                                    .padding(top = 2.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceTonal)
                            .clickable { displayedMonth = displayedMonth.plusMonths(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Mes siguiente",
                            tint = colors.inkPrimary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Weekday Column Headers + Weekly Status Column
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("L", "M", "X", "J", "V", "S", "D").forEach { d ->
                        Text(
                            text = d,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.inkTertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = "OBJ",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inkTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(44.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Week Rows (Mon-Sun)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    weeks.forEach { week ->
                        CalendarWeekBandRow(
                            week = week,
                            onTapDay = { cell ->
                                if (cell.hasWorkout) selectedDate = cell.date
                            }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Clean Color Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = colors.success, label = "Cumplida")
                    LegendDot(color = colors.danger, label = "Sin objetivo")
                    LegendDot(color = colors.accentText, label = "En curso")
                }
            }
        }

        // Inline Weekly Goal Quick Adjuster
        item {
            KovaCard(contentPadding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Objetivo semanal",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.inkPrimary
                        )
                        Text(
                            text = "Mínimo de días por semana (lunes a domingo)",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary
                        )
                    }
                    KovaStepper(
                        value = uiState.preferences.weeklyGoalDays,
                        minValue = 1,
                        maxValue = 7,
                        onValueChange = onUpdateWeeklyGoal
                    )
                }
            }
        }
    }

    selectedDate?.let { date ->
        val liveSessions = remember(date, finishedSessions) {
            finishedSessions.filter { it.date == date }
        }
        androidx.compose.runtime.LaunchedEffect(liveSessions.isEmpty()) {
            if (liveSessions.isEmpty()) {
                selectedDate = null
            }
        }
        if (liveSessions.isNotEmpty()) {
            CalendarDayDetailSheet(
                date = date,
                sessions = liveSessions,
                unit = uiState.preferences.weightUnit,
                setsForSessionProvider = setsForSessionProvider,
                onDeleteSession = onDeleteSession,
                onDismiss = { selectedDate = null }
            )
        }
    }
}

@Composable
private fun CalendarWeekBandRow(
    week: CalendarWeekSummary,
    onTapDay: (CalendarDayCell) -> Unit
) {
    val colors = KovaTheme.colors
    val bandBg = when (week.status) {
        WeekStatus.MET -> colors.successSoft
        WeekStatus.MISSED -> colors.dangerSoft
        WeekStatus.IN_PROGRESS -> if (week.isMathematicallyMissed) colors.warningSoft else colors.surfaceTonal.copy(alpha = 0.65f)
        WeekStatus.BEFORE_TRACKING, WeekStatus.FUTURE -> Color.Transparent
    }
    val bandBorder = when (week.status) {
        WeekStatus.MET -> colors.successBorder
        WeekStatus.MISSED -> colors.dangerBorder
        WeekStatus.IN_PROGRESS -> if (week.isMathematicallyMissed) colors.warningBorder else colors.borderSubtle
        WeekStatus.BEFORE_TRACKING, WeekStatus.FUTURE -> Color.Transparent
    }
    val badgeColor = when (week.status) {
        WeekStatus.MET -> colors.success
        WeekStatus.MISSED -> colors.danger
        WeekStatus.IN_PROGRESS -> if (week.isMathematicallyMissed) colors.warning else colors.inkSecondary
        WeekStatus.BEFORE_TRACKING, WeekStatus.FUTURE -> colors.inkTertiary
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bandBg,
        border = if (bandBorder != Color.Transparent) BorderStroke(1.dp, bandBorder) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            week.days.forEach { cell ->
                val dayAlpha = if (cell.inCurrentMonth) 1f else 0.35f
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(dayAlpha)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = cell.hasWorkout) { onTapDay(cell) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    cell.hasWorkout -> colors.success
                                    cell.isToday -> colors.accentSoft
                                    else -> Color.Transparent
                                }
                            )
                            .then(
                                if (cell.isToday && !cell.hasWorkout) Modifier.border(
                                    1.5.dp,
                                    colors.accentText,
                                    CircleShape
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${cell.date.dayOfMonth}",
                            style = MaterialTheme.typography.labelMedium.tabular().copy(
                                fontWeight = if (cell.hasWorkout || cell.isToday) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = when {
                                cell.hasWorkout -> Color.White
                                cell.isToday -> colors.accentText
                                else -> colors.inkPrimary
                            }
                        )
                    }

                    // Tiny routine color dot below trained day
                    if (cell.hasWorkout) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(cell.sessions.first().routineColorHexSnapshot))
                        )
                    } else {
                        Spacer(Modifier.height(5.dp))
                    }
                }
            }

            // Weekly Goal Fraction Badge
            Box(
                modifier = Modifier.width(44.dp),
                contentAlignment = Alignment.Center
            ) {
                val label = when (week.status) {
                    WeekStatus.BEFORE_TRACKING, WeekStatus.FUTURE -> "—"
                    else -> "${week.daysTrained}/${week.weeklyGoal}"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.tabular().copy(fontWeight = FontWeight.Bold),
                    color = badgeColor
                )
            }
        }
    }
}

@Composable
private fun CalendarStatChip(
    value: String,
    label: String,
    tint: Color,
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.tabular().copy(fontWeight = FontWeight.Bold),
                color = tint
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = KovaTheme.colors.inkSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDayDetailSheet(
    date: LocalDate,
    sessions: List<WorkoutSession>,
    unit: WeightUnit,
    setsForSessionProvider: (Long) -> List<WorkoutSet>,
    onDeleteSession: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = KovaTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sessionPendingDelete by remember { mutableStateOf<WorkoutSession?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = date.format(dayDetailFormatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineSmall,
                color = colors.inkPrimary
            )

            sessions.forEach { session ->
                val sets = setsForSessionProvider(session.id).filter { it.isCompleted && it.reps > 0 }
                val byExercise = sets.groupBy { it.exerciseNameSnapshot }
                val totalVol = sets.sumOf { it.weightKg * it.reps }
                val durationMin = (((session.finishedAtEpochMillis ?: session.startedAtEpochMillis) - session.startedAtEpochMillis) / 60_000L).coerceAtLeast(1L)

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceTonal,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(session.routineColorHexSnapshot))
                                )
                                Text(
                                    text = session.routineDayNameSnapshot,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.inkPrimary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                KovaStatusPill(
                                    text = "${session.completedExerciseCount}/${session.targetExerciseCountSnapshot}" + if (session.isDayGoalMet) " ✓" else "",
                                    containerColor = if (session.isDayGoalMet) colors.successSoft else colors.surface,
                                    contentColor = if (session.isDayGoalMet) colors.success else colors.inkSecondary
                                )
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .clickable { sessionPendingDelete = session },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Eliminar sesión",
                                        tint = colors.inkTertiary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "$durationMin min · ${sets.size} series · ${WeightFormatter.formatVolume(totalVol, unit)}",
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = colors.inkSecondary
                        )

                        byExercise.forEach { (exerciseName, exSets) ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surface)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = exerciseName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.inkPrimary
                                )
                                Text(
                                    text = exSets.joinToString("  ·  ") { s ->
                                        "${WeightFormatter.formatWeight(s.weightKg, unit, false)} × ${s.reps}"
                                    },
                                    style = MaterialTheme.typography.bodySmall.tabular(),
                                    color = colors.inkSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    sessionPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { sessionPendingDelete = null },
            containerColor = colors.surface,
            title = { Text("¿Eliminar esta sesión?", color = colors.inkPrimary) },
            text = {
                Text(
                    "Se eliminará el registro de ${target.routineDayNameSnapshot} de este día.",
                    color = colors.inkSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = target.id
                    sessionPendingDelete = null
                    onDeleteSession(id)
                }) {
                    Text("Eliminar", color = colors.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionPendingDelete = null }) {
                    Text("Cancelar", color = colors.inkSecondary)
                }
            }
        )
    }
}
