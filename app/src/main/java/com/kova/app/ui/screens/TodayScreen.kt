package com.kova.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kova.app.domain.logic.DayGoalEvaluator
import com.kova.app.domain.logic.WeightFormatter
import com.kova.app.domain.model.WeekStatus
import com.kova.app.domain.model.WorkoutSession
import com.kova.app.ui.components.KovaCard
import com.kova.app.ui.components.KovaLogoMark
import com.kova.app.ui.components.KovaPrimaryButton
import com.kova.app.ui.components.KovaSectionHeader
import com.kova.app.ui.components.KovaStatusPill
import com.kova.app.ui.components.ProgressRing
import com.kova.app.ui.components.parseHexColor
import com.kova.app.ui.state.KovaUiState
import com.kova.app.ui.theme.KovaTheme
import com.kova.app.ui.theme.tabular
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanishLocale = Locale.forLanguageTag("es-ES")
private val todayHeaderFormatter = DateTimeFormatter.ofPattern("EEEE · d MMM", spanishLocale)
private val shortHistoryDateFormatter = DateTimeFormatter.ofPattern("d MMM", spanishLocale)

@Composable
fun TodayScreen(
    uiState: KovaUiState,
    onStartWorkout: (routineDayId: Long) -> Unit,
    onResumeActiveWorkout: () -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenRoutineDetail: (Long) -> Unit
) {
    val colors = KovaTheme.colors
    val today = LocalDate.now()
    val accentHighlight = Color(uiState.preferences.accentPalette.previewHexDark)
    val finishedSessions = remember(uiState.allSessions) {
        uiState.allSessions.filter { !it.isActive }.take(4)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 14.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Editorial Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = today.format(todayHeaderFormatter).uppercase(spanishLocale),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inkSecondary
                    )
                    Text(
                        text = "Hoy",
                        style = MaterialTheme.typography.displayLarge,
                        color = colors.inkPrimary
                    )
                }
                KovaLogoMark(
                    size = 42.dp,
                    accentCollarColor = accentHighlight
                )
            }
        }

        // Active Workout Live Banner
        uiState.activeSession?.let { active ->
            item {
                val routineExercises = uiState.routines
                    .firstOrNull { it.routine.id == active.session.routineDayId }
                    ?.activeExercises.orEmpty()
                val progress = DayGoalEvaluator.evaluate(
                    routineExercises = routineExercises,
                    targetExerciseCount = active.session.targetExerciseCountSnapshot,
                    sessionSets = active.sets
                )
                ActiveSessionBanner(
                    routineName = active.session.routineDayNameSnapshot,
                    routineColorHex = active.session.routineColorHexSnapshot,
                    accentHighlight = accentHighlight,
                    startedAtMillis = active.session.startedAtEpochMillis,
                    completedCount = progress.totalCompleted,
                    targetCount = progress.targetCount,
                    onResume = onResumeActiveWorkout
                )
            }
        }

        // Hero Suggested Routine Card
        item {
            val suggested = uiState.suggestedRoutine
            val suggestedWrapper = uiState.routines.firstOrNull { it.routine.id == suggested?.id }
            if (suggested != null && suggestedWrapper != null) {
                val mandatoryCount = suggestedWrapper.mandatoryCount
                val optionalCount = suggestedWrapper.optionalCount
                val target = maxOf(suggested.targetExerciseCount, mandatoryCount)
                val optionalNeeded = (target - mandatoryCount).coerceAtLeast(0)
                val dotColor = parseHexColor(suggested.colorHex)

                KovaCard(
                    contentPadding = PaddingValues(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KovaStatusPill(
                            text = "Siguiente en rotación",
                            containerColor = colors.accentSoft,
                            contentColor = colors.accentText,
                            leadingDotColor = dotColor
                        )
                        Text(
                            text = "Configurar",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.inkSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .clickable { onOpenRoutineDetail(suggested.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = suggested.name,
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.inkPrimary
                    )

                    Spacer(Modifier.height(6.dp))

                    val subtitle = when {
                        suggestedWrapper.activeExercises.isEmpty() ->
                            "Añade ejercicios a este día para empezar a registrar"
                        suggestedWrapper.activeExercises.size < target ->
                            "Objetivo: $target ejercicios · Tienes ${suggestedWrapper.activeExercises.size} añadidos"
                        optionalNeeded > 0 ->
                            "Objetivo: $target ejercicios · $mandatoryCount obligatorios + $optionalNeeded de $optionalCount opcionales"
                        else ->
                            "Objetivo: $target ejercicios · $mandatoryCount obligatorios"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkSecondary
                    )

                    Spacer(Modifier.height(20.dp))

                    val hasActiveOther = uiState.activeSession != null
                    KovaPrimaryButton(
                        text = if (hasActiveOther) "Volver al entreno en curso" else "Empezar ${suggested.name}",
                        icon = Icons.Rounded.PlayArrow,
                        onClick = {
                            if (hasActiveOther) onResumeActiveWorkout()
                            else onStartWorkout(suggested.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.routines.size > 1 && !hasActiveOther) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "O cambiar:",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkTertiary
                            )
                            uiState.routines
                                .filter { it.routine.id != suggested.id }
                                .forEach { other ->
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = colors.surfaceTonal,
                                        border = BorderStroke(1.dp, colors.border),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(100.dp))
                                            .clickable { onStartWorkout(other.routine.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(parseHexColor(other.routine.colorHex))
                                            )
                                            Text(
                                                text = other.routine.name,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = colors.inkPrimary
                                            )
                                        }
                                    }
                                }
                        }
                    }
                }
            } else {
                KovaCard(
                    contentPadding = PaddingValues(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Crea tu primera rutina",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.inkPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Añade días como Push, Pull o Leg y define tus ejercicios obligatorios y opcionales.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    KovaPrimaryButton(
                        text = "Ir a Rutinas",
                        icon = Icons.Rounded.Add,
                        onClick = onOpenRoutines,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Weekly Ring + Streak Card
        uiState.currentWeekSummary?.let { week ->
            item {
                val met = week.status == WeekStatus.MET
                val ringColor = when {
                    met -> colors.success
                    week.isMathematicallyMissed -> colors.warning
                    else -> if (colors.isDark) colors.accent else colors.accentText
                }
                val progress = (week.daysTrained.toFloat() / week.weeklyGoal.toFloat()).coerceIn(0f, 1f)
                val dayLabels = listOf("L", "M", "X", "J", "V", "S", "D")

                KovaCard(
                    contentPadding = PaddingValues(20.dp),
                    onClick = onOpenCalendar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        ProgressRing(
                            progress = progress,
                            ringColor = ringColor,
                            trackColor = colors.surfaceTonal,
                            size = 82.dp,
                            strokeWidth = 8.dp
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${week.daysTrained}/${week.weeklyGoal}",
                                    style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                                    color = colors.inkPrimary
                                )
                                Text(
                                    text = "días",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.inkTertiary
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ESTA SEMANA",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.inkSecondary
                            )
                            Text(
                                text = when {
                                    met -> "¡Objetivo cumplido!"
                                    week.isMathematicallyMissed -> "Semana justa · suma lo que puedas"
                                    else -> {
                                        val left = (week.weeklyGoal - week.daysTrained).coerceAtLeast(1)
                                        "Te ${if (left == 1) "falta 1 día" else "faltan $left días"}"
                                    }
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = if (met) colors.success else colors.inkPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = if (uiState.weeklyStreak > 0) colors.warning else colors.inkTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${uiState.weeklyStreak} sem. racha",
                                        style = MaterialTheme.typography.labelMedium.tabular(),
                                        color = colors.inkSecondary
                                    )
                                }
                                Text(
                                    text = "· ${uiState.thisMonthWorkoutCount} este mes",
                                    style = MaterialTheme.typography.labelMedium.tabular(),
                                    color = colors.inkTertiary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 7-day miniature week bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.days.forEachIndexed { idx, cell ->
                            val trained = cell.hasWorkout
                            val bg = when {
                                trained -> colors.success
                                cell.isToday -> colors.accentSoft
                                else -> colors.surfaceTonal
                            }
                            val textCol = when {
                                trained -> Color.White
                                cell.isToday -> colors.accentText
                                else -> colors.inkSecondary
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(bg)
                                    .then(
                                        if (cell.isToday && !trained) Modifier.border(
                                            1.5.dp,
                                            colors.accentText,
                                            CircleShape
                                        ) else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (trained) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = dayLabels[idx],
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = dayLabels[idx],
                                        style = MaterialTheme.typography.labelMedium,
                                        color = textCol
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Workouts Section
        if (finishedSessions.isNotEmpty()) {
            item {
                KovaSectionHeader(
                    title = "Últimas sesiones",
                    trailing = {
                        Text(
                            text = "Ver calendario",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accentText,
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .clickable(onClick = onOpenCalendar)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                )
            }
            item {
                KovaCard(contentPadding = PaddingValues(vertical = 6.dp)) {
                    finishedSessions.forEachIndexed { idx, session ->
                        RecentSessionRow(
                            session = session,
                            onClick = onOpenCalendar
                        )
                        if (idx < finishedSessions.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(1.dp)
                                    .background(colors.borderSubtle)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveSessionBanner(
    routineName: String,
    routineColorHex: String,
    accentHighlight: Color,
    startedAtMillis: Long,
    completedCount: Int,
    targetCount: Int,
    onResume: () -> Unit
) {
    val colors = KovaTheme.colors
    var elapsedSec by remember(startedAtMillis) {
        mutableLongStateOf(((System.currentTimeMillis() - startedAtMillis) / 1000L).coerceAtLeast(0L))
    }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            elapsedSec = ((System.currentTimeMillis() - startedAtMillis) / 1000L).coerceAtLeast(0L)
            delay(1000L)
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.islandBg,
        border = BorderStroke(1.dp, Color(0xFF2B303C)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onResume)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
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
                        .background(parseHexColor(routineColorHex).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = parseHexColor(routineColorHex),
                        modifier = Modifier.size(21.dp)
                    )
                }
                Column {
                    Text(
                        text = "$routineName en curso",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.islandInk
                    )
                    Text(
                        text = "$completedCount/$targetCount ejercicios · ${WeightFormatter.formatDuration(elapsedSec)}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.islandInkMuted
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = accentHighlight
            ) {
                Text(
                    text = "Seguir",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0E1203),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentSessionRow(
    session: WorkoutSession,
    onClick: () -> Unit
) {
    val colors = KovaTheme.colors
    val durationSec = ((session.finishedAtEpochMillis ?: session.startedAtEpochMillis) - session.startedAtEpochMillis) / 1000L
    val mins = (durationSec / 60L).coerceAtLeast(1L)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(session.routineColorHexSnapshot))
            )
            Column {
                Text(
                    text = session.routineDayNameSnapshot,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.inkPrimary
                )
                Text(
                    text = "${session.date.format(shortHistoryDateFormatter)} · ${mins} min",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = colors.inkSecondary
                )
            }
        }

        KovaStatusPill(
            text = "${session.completedExerciseCount}/${session.targetExerciseCountSnapshot}" + if (session.isDayGoalMet) " ✓" else "",
            containerColor = if (session.isDayGoalMet) colors.successSoft else colors.surfaceTonal,
            contentColor = if (session.isDayGoalMet) colors.success else colors.inkSecondary
        )
    }
}
