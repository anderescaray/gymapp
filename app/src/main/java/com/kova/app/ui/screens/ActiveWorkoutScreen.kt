package com.kova.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kova.app.data.repository.FinishedWorkoutSummary
import com.kova.app.domain.logic.DayGoalEvaluator
import com.kova.app.domain.logic.WeightFormatter
import com.kova.app.domain.model.Exercise
import com.kova.app.domain.model.RoutineWithExercises
import com.kova.app.domain.model.SessionWithSets
import com.kova.app.domain.model.WeightUnit
import com.kova.app.domain.model.WorkoutSet
import com.kova.app.ui.components.ExerciseEditorSheet
import com.kova.app.ui.components.KovaCard
import com.kova.app.ui.components.KovaPrimaryButton
import com.kova.app.ui.components.KovaSecondaryButton
import com.kova.app.ui.components.KovaSectionHeader
import com.kova.app.ui.components.KovaStatusPill
import com.kova.app.ui.components.parseHexColor
import com.kova.app.ui.theme.KovaTheme
import com.kova.app.ui.theme.tabular
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutScreen(
    activeSession: SessionWithSets?,
    routineWrapper: RoutineWithExercises?,
    unit: WeightUnit,
    defaultRestSeconds: Int,
    canDrawOverlays: Boolean,
    livePrExerciseIds: Set<Long>,
    lastSessionSetsProvider: (Long) -> List<WorkoutSet>,
    onBack: () -> Unit,
    onAddSet: (sessionId: Long, exercise: Exercise, weightKg: Double, reps: Int) -> Unit,
    onCopyLastSession: (sessionId: Long, exercise: Exercise) -> Unit,
    onUpdateSet: (set: WorkoutSet, exercise: Exercise, justCompleted: Boolean) -> Unit,
    onDeleteSet: (WorkoutSet) -> Unit,
    onCreateExerciseInRoutine: (routineDayId: Long, name: String, isMandatory: Boolean, restSeconds: Int?) -> Unit,
    onStartManualTimer: (seconds: Int, label: String) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenExerciseAnalytics: (Long) -> Unit,
    onFinishSession: () -> Unit,
    onDiscardSession: () -> Unit
) {
    val colors = KovaTheme.colors
    if (activeSession == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.canvas),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = colors.accentText,
                strokeWidth = 3.dp
            )
        }
        return
    }

    val session = activeSession.session
    val routineExercises = routineWrapper?.activeExercises.orEmpty()
    val mandatoryExercises = remember(routineExercises) { routineExercises.filter { it.isMandatory } }
    val optionalExercises = remember(routineExercises) { routineExercises.filterNot { it.isMandatory } }

    val dayStatus = remember(routineExercises, session.targetExerciseCountSnapshot, activeSession.sets) {
        DayGoalEvaluator.evaluate(
            routineExercises = routineExercises,
            targetExerciseCount = session.targetExerciseCountSnapshot,
            sessionSets = activeSession.sets
        )
    }

    var expandedExerciseId by rememberSaveable(session.id) {
        mutableLongStateOf(routineExercises.firstOrNull()?.id ?: -1L)
    }
    var elapsedSec by remember(session.startedAtEpochMillis) {
        mutableLongStateOf(((System.currentTimeMillis() - session.startedAtEpochMillis) / 1000L).coerceAtLeast(0L))
    }
    LaunchedEffect(session.startedAtEpochMillis) {
        while (true) {
            elapsedSec = ((System.currentTimeMillis() - session.startedAtEpochMillis) / 1000L).coerceAtLeast(0L)
            delay(1000L)
        }
    }

    var showFinishConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var showAddExerciseSheet by remember { mutableStateOf(false) }
    var dismissedOverlayHint by rememberSaveable { mutableStateOf(false) }

    val activeExercise = routineExercises.firstOrNull { it.id == expandedExerciseId }
        ?: routineExercises.firstOrNull()
    val manualRestSeconds = activeExercise?.defaultRestSeconds ?: defaultRestSeconds
    val manualRestLabel = activeExercise?.name ?: session.routineDayNameSnapshot

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Sticky Workout Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                        contentDescription = "Minimizar",
                        tint = colors.inkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(session.routineColorHexSnapshot))
                        )
                        Text(
                            text = session.routineDayNameSnapshot,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.inkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = WeightFormatter.formatDuration(elapsedSec),
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.inkSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .clickable { onStartManualTimer(manualRestSeconds, manualRestLabel) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "Iniciar descanso",
                            tint = colors.accentText,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "${manualRestSeconds}s",
                            style = MaterialTheme.typography.labelMedium.tabular().copy(fontWeight = FontWeight.SemiBold),
                            color = colors.inkPrimary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (dayStatus.isComplete) colors.success else colors.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .clickable { showFinishConfirm = true }
                ) {
                    Text(
                        text = "Finalizar",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (dayStatus.isComplete) Color.White else colors.onAccent,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Day Completion Progress Banner
            item {
                val barTint by animateColorAsState(
                    targetValue = if (dayStatus.isComplete) colors.success else if (colors.isDark) colors.accent else colors.accentText,
                    label = "dayProgTint"
                )
                KovaCard(
                    containerColor = if (dayStatus.isComplete) colors.successSoft else colors.surface,
                    borderColor = if (dayStatus.isComplete) colors.successBorder else colors.border,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dayStatus.statusHeadline,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (dayStatus.isComplete) colors.success else colors.inkPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Obligatorios: ${dayStatus.mandatoryCompleted}/${dayStatus.mandatoryTotal}  ·  Opcionales: ${dayStatus.optionalCompleted}/${dayStatus.optionalRequired}",
                                style = MaterialTheme.typography.bodySmall.tabular(),
                                color = colors.inkSecondary
                            )
                        }
                        Text(
                            text = "${dayStatus.totalCompleted}/${dayStatus.targetCount}",
                            style = MaterialTheme.typography.headlineSmall.tabular().copy(fontWeight = FontWeight.Bold),
                            color = if (dayStatus.isComplete) colors.success else colors.inkPrimary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { dayStatus.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(100.dp)),
                        color = barTint,
                        trackColor = colors.surfaceTonal,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            if (!canDrawOverlays && !dismissedOverlayHint) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onRequestOverlayPermission)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureInPictureAlt,
                                contentDescription = null,
                                tint = colors.accentText,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Ver descanso flotante sobre otras apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            KovaStatusPill(
                                text = "Activar",
                                containerColor = colors.accentSoft,
                                contentColor = colors.accentText
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { dismissedOverlayHint = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Ocultar",
                                    tint = colors.inkTertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Mandatory Exercises
            if (mandatoryExercises.isNotEmpty()) {
                item {
                    KovaSectionHeader(
                        title = "Obligatorios (${dayStatus.mandatoryCompleted}/${dayStatus.mandatoryTotal})",
                        trailing = {
                            if (dayStatus.mandatoryRemaining == 0) {
                                Text(
                                    text = "✓ Completados",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.success
                                )
                            }
                        }
                    )
                }
                items(mandatoryExercises, key = { it.id }) { ex ->
                    val setsForEx = activeSession.sets
                        .filter { it.exerciseId == ex.id }
                        .sortedBy { it.setIndex }
                    val lastSets = remember(ex.id, session.id) { lastSessionSetsProvider(ex.id) }
                    ActiveExerciseAccordionCard(
                        exercise = ex,
                        sets = setsForEx,
                        lastSessionSets = lastSets,
                        unit = unit,
                        isExpanded = expandedExerciseId == ex.id,
                        hasLivePr = ex.id in livePrExerciseIds,
                        onToggleExpand = {
                            expandedExerciseId = if (expandedExerciseId == ex.id) -1L else ex.id
                        },
                        onOpenAnalytics = { onOpenExerciseAnalytics(ex.id) },
                        onCopyLastSession = { onCopyLastSession(session.id, ex) },
                        onAddSet = { weightKg, reps -> onAddSet(session.id, ex, weightKg, reps) },
                        onUpdateSet = { set, justDone -> onUpdateSet(set, ex, justDone) },
                        onDeleteSet = onDeleteSet
                    )
                }
            }

            // Optional Exercises
            if (optionalExercises.isNotEmpty()) {
                item {
                    KovaSectionHeader(
                        title = "Opcionales (${dayStatus.optionalCompleted}/${dayStatus.optionalRequired})",
                        trailing = {
                            val hint = if (dayStatus.optionalRemaining == 0) "✓ Listo · puedes añadir extras"
                            else "Elige ${dayStatus.optionalRemaining} más"
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (dayStatus.optionalRemaining == 0) colors.success else colors.inkSecondary
                            )
                        }
                    )
                }
                items(optionalExercises, key = { it.id }) { ex ->
                    val setsForEx = activeSession.sets
                        .filter { it.exerciseId == ex.id }
                        .sortedBy { it.setIndex }
                    val lastSets = remember(ex.id, session.id) { lastSessionSetsProvider(ex.id) }
                    ActiveExerciseAccordionCard(
                        exercise = ex,
                        sets = setsForEx,
                        lastSessionSets = lastSets,
                        unit = unit,
                        isExpanded = expandedExerciseId == ex.id,
                        hasLivePr = ex.id in livePrExerciseIds,
                        onToggleExpand = {
                            expandedExerciseId = if (expandedExerciseId == ex.id) -1L else ex.id
                        },
                        onOpenAnalytics = { onOpenExerciseAnalytics(ex.id) },
                        onCopyLastSession = { onCopyLastSession(session.id, ex) },
                        onAddSet = { weightKg, reps -> onAddSet(session.id, ex, weightKg, reps) },
                        onUpdateSet = { set, justDone -> onUpdateSet(set, ex, justDone) },
                        onDeleteSet = onDeleteSet
                    )
                }
            }

            // Footer Actions
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (session.routineDayId != null) {
                        KovaSecondaryButton(
                            text = "Añadir ejercicio a ${session.routineDayNameSnapshot}",
                            icon = Icons.Rounded.Add,
                            onClick = { showAddExerciseSheet = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    TextButton(onClick = { showDiscardConfirm = true }) {
                        Text(
                            text = "Descartar entrenamiento",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.danger
                        )
                    }
                }
            }
        }
    }

    if (showFinishConfirm) {
        val totalSetsDone = activeSession.completedSets.size
        val uncheckedCount = activeSession.sets.count { !it.isCompleted || it.reps <= 0 }
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = when {
                        totalSetsDone == 0 -> "Aún no has marcado ninguna serie"
                        dayStatus.isComplete -> "¿Finalizar y guardar sesión?"
                        else -> "¿Finalizar antes del objetivo?"
                    },
                    color = colors.inkPrimary
                )
            },
            text = {
                Text(
                    text = when {
                        totalSetsDone == 0 ->
                            "Marca al menos una serie con el check verde para guardar el entrenamiento, o descártalo."
                        dayStatus.isComplete && uncheckedCount > 0 ->
                            "${dayStatus.statusHeadline}. Se guardarán tus $totalSetsDone series completadas (las $uncheckedCount series sin marcar se omitirán)."
                        dayStatus.isComplete ->
                            "${dayStatus.statusHeadline}. Se guardarán tus $totalSetsDone series y se actualizarán tus gráficas y calendario."
                        else ->
                            "${dayStatus.statusHeadline}. Se guardarán las $totalSetsDone series que ya completaste."
                    },
                    color = colors.inkSecondary
                )
            },
            confirmButton = {
                if (totalSetsDone > 0) {
                    TextButton(onClick = {
                        showFinishConfirm = false
                        onFinishSession()
                    }) {
                        Text(
                            text = if (dayStatus.isComplete) "Guardar entreno" else "Guardar sesión",
                            color = if (dayStatus.isComplete) colors.success else colors.accentText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    TextButton(onClick = {
                        showFinishConfirm = false
                        onDiscardSession()
                    }) {
                        Text("Descartar", color = colors.danger, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirm = false }) {
                    Text("Seguir entrenando", color = colors.inkSecondary)
                }
            }
        )
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            containerColor = colors.surface,
            title = { Text("¿Descartar este entreno?", color = colors.inkPrimary) },
            text = {
                Text(
                    "Se perderán las series registradas en esta sesión activa.",
                    color = colors.inkSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirm = false
                    onDiscardSession()
                }) {
                    Text("Descartar", color = colors.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("Cancelar", color = colors.inkSecondary)
                }
            }
        )
    }

    if (showAddExerciseSheet && session.routineDayId != null) {
        ExerciseEditorSheet(
            existing = null,
            defaultRestSeconds = defaultRestSeconds,
            onDismiss = { showAddExerciseSheet = false },
            onSave = { name, mandatory, rest ->
                onCreateExerciseInRoutine(session.routineDayId, name, mandatory, rest)
                showAddExerciseSheet = false
            }
        )
    }
}

@Composable
private fun ActiveExerciseAccordionCard(
    exercise: Exercise,
    sets: List<WorkoutSet>,
    lastSessionSets: List<WorkoutSet>,
    unit: WeightUnit,
    isExpanded: Boolean,
    hasLivePr: Boolean,
    onToggleExpand: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onCopyLastSession: () -> Unit,
    onAddSet: (weightKg: Double, reps: Int) -> Unit,
    onUpdateSet: (WorkoutSet, Boolean) -> Unit,
    onDeleteSet: (WorkoutSet) -> Unit
) {
    val colors = KovaTheme.colors
    val haptics = LocalHapticFeedback.current
    val completedSets = sets.filter { it.isCompleted && it.reps > 0 }
    val isDone = completedSets.isNotEmpty()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = when {
                isDone && !isExpanded -> colors.successBorder
                isExpanded -> colors.accentText.copy(alpha = 0.55f)
                else -> colors.border
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Exercise Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDone) colors.success else colors.surfaceTonal),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Completado",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (exercise.isMandatory) colors.accentText else colors.inkTertiary)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.inkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (hasLivePr) {
                            KovaStatusPill(
                                text = "PR",
                                containerColor = colors.successSoft,
                                contentColor = colors.success
                            )
                        }
                    }

                    val subText = when {
                        completedSets.isNotEmpty() -> {
                            val best = completedSets.maxBy { it.weightKg * 100 + it.reps }
                            "${completedSets.size} ${if (completedSets.size == 1) "serie" else "series"} · ${WeightFormatter.formatWeight(best.weightKg, unit, false)} × ${best.reps}"
                        }
                        lastSessionSets.isNotEmpty() -> {
                            val first = lastSessionSets.first()
                            "Última vez: ${WeightFormatter.formatWeight(first.weightKg, unit, false)} × ${first.reps}"
                        }
                        else -> "Toca para registrar series"
                    }
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = if (isDone) colors.success else colors.inkSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Chart Quick Access Icon
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceTonal)
                        .clickable(onClick = onOpenAnalytics),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoGraph,
                        contentDescription = "Ver gráfica",
                        tint = colors.inkSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = colors.inkTertiary
                )
            }

            // Expanded Set Logger Body
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.borderSubtle)
                    )

                    // Last Session Ghost Banner + 1-Tap Copy
                    if (lastSessionSets.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Última vez: " + lastSessionSets.take(3).joinToString(" · ") {
                                    "${WeightFormatter.formatCompactWeight(it.weightKg, unit, false)}×${it.reps}"
                                },
                                style = MaterialTheme.typography.bodySmall.tabular(),
                                color = colors.inkTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (sets.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = colors.accentSoft,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onCopyLastSession()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = null,
                                            tint = colors.accentText,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Copiar (${lastSessionSets.size})",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.accentText
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Set Rows
                    if (sets.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SERIE",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.inkTertiary,
                                modifier = Modifier.width(34.dp)
                            )
                            Text(
                                text = unit.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.inkTertiary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "REPS",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.inkTertiary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.85f)
                            )
                            Spacer(Modifier.width(76.dp))
                        }
                    }

                    sets.forEachIndexed { idx, set ->
                        val prevReference = lastSessionSets.getOrNull(idx)
                        WorkoutSetRowItem(
                            set = set,
                            previousSet = prevReference,
                            unit = unit,
                            onUpdate = onUpdateSet,
                            onDelete = { onDeleteSet(set) }
                        )
                    }

                    // Add Set Button (auto-populates from last set in current session or historical session)
                    val nextIndex = sets.size
                    val seedWeight = sets.lastOrNull()?.weightKg
                        ?: lastSessionSets.getOrNull(nextIndex)?.weightKg
                        ?: lastSessionSets.lastOrNull()?.weightKg
                        ?: 20.0
                    val seedReps = sets.lastOrNull()?.reps?.takeIf { it > 0 }
                        ?: lastSessionSets.getOrNull(nextIndex)?.reps
                        ?: lastSessionSets.lastOrNull()?.reps
                        ?: 8

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceTonal,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAddSet(seedWeight, seedReps)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 11.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = colors.inkPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Añadir serie ${sets.size + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.inkPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSetRowItem(
    set: WorkoutSet,
    previousSet: WorkoutSet?,
    unit: WeightUnit,
    onUpdate: (WorkoutSet, Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val colors = KovaTheme.colors
    val haptics = LocalHapticFeedback.current

    val displayWeight = WeightFormatter.kgToDisplay(set.weightKg, unit)
    var weightText by remember(set.id, unit) {
        mutableStateOf(WeightFormatter.formatNumber(displayWeight))
    }
    var repsText by remember(set.id) {
        mutableStateOf(if (set.reps > 0) set.reps.toString() else "")
    }
    var isWeightFocused by remember(set.id) { mutableStateOf(false) }
    var isRepsFocused by remember(set.id) { mutableStateOf(false) }

    fun currentLocalWeightKg(): Double {
        val parsed = weightText.toDoubleOrNull()?.coerceIn(0.0, 2000.0) ?: 0.0
        return WeightFormatter.displayToKg(parsed, unit)
    }

    fun currentLocalReps(): Int {
        return (repsText.toIntOrNull() ?: 0).coerceIn(0, 200)
    }

    // Sync only when NOT actively typing in that field (prevents eating decimals, clearing, or LB rounding jumps)
    LaunchedEffect(displayWeight, isWeightFocused) {
        if (!isWeightFocused) {
            val currentParsed = weightText.toDoubleOrNull()
            val diverges = when {
                weightText.isEmpty() && displayWeight == 0.0 -> false
                currentParsed == null -> displayWeight > 0.0
                else -> kotlin.math.abs(currentParsed - displayWeight) > 0.01
            }
            if (diverges) {
                weightText = WeightFormatter.formatNumber(displayWeight)
            }
        }
    }
    LaunchedEffect(set.reps, isRepsFocused) {
        if (!isRepsFocused) {
            val currentParsed = repsText.toIntOrNull()
            val diverges = when {
                repsText.isEmpty() && set.reps == 0 -> false
                currentParsed == null -> set.reps > 0
                else -> currentParsed != set.reps
            }
            if (diverges) {
                repsText = if (set.reps > 0) set.reps.toString() else ""
            }
        }
    }

    val rowBg by animateColorAsState(
        targetValue = if (set.isCompleted) colors.successSoft else colors.surfaceTonal.copy(alpha = 0.65f),
        label = "setRowBg"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Set index badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (set.isCompleted) colors.success.copy(alpha = 0.18f) else colors.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${set.setIndex}",
                    style = MaterialTheme.typography.labelMedium.tabular().copy(fontWeight = FontWeight.Bold),
                    color = if (set.isCompleted) colors.success else colors.inkPrimary
                )
            }

            // Weight Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, if (isWeightFocused) colors.accentText else colors.border),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    BasicTextField(
                        value = weightText,
                        onValueChange = { raw ->
                            val clean = raw.replace(',', '.').filter { it.isDigit() || it == '.' }.take(6)
                            if (clean.count { it == '.' } <= 1) {
                                val parsed = clean.toDoubleOrNull()
                                if (parsed == null || parsed <= 2000.0) {
                                    weightText = clean
                                    val kg = WeightFormatter.displayToKg(parsed ?: 0.0, unit)
                                    val localReps = currentLocalReps()
                                    onUpdate(set.copy(weightKg = kg, reps = localReps), false)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.titleMedium.tabular().copy(
                            color = colors.inkPrimary,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(colors.accentText),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isWeightFocused = it.isFocused }
                    )
                }
            }

            // Reps Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, if (isRepsFocused) colors.accentText else colors.border),
                modifier = Modifier
                    .weight(0.85f)
                    .height(42.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    BasicTextField(
                        value = repsText,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }.take(3)
                            repsText = clean
                            val parsed = (clean.toIntOrNull() ?: 0).coerceIn(0, 200)
                            val stillCompleted = set.isCompleted && parsed > 0
                            onUpdate(
                                set.copy(
                                    weightKg = currentLocalWeightKg(),
                                    reps = parsed,
                                    isCompleted = stillCompleted
                                ),
                                false
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.titleMedium.tabular().copy(
                            color = colors.inkPrimary,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(colors.accentText),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isRepsFocused = it.isFocused }
                    )
                }
            }

            // Complete Set Check Circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (set.isCompleted) colors.success else colors.surface)
                    .clickable {
                        val nextDone = !set.isCompleted
                        val effectiveReps = if (nextDone) currentLocalReps().coerceAtLeast(1) else currentLocalReps()
                        if (nextDone && repsText.isBlank()) {
                            repsText = effectiveReps.toString()
                        }
                        val effectiveKg = currentLocalWeightKg()
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onUpdate(
                            set.copy(
                                weightKg = effectiveKg,
                                reps = effectiveReps,
                                isCompleted = nextDone
                            ),
                            nextDone
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = if (set.isCompleted) "Desmarcar serie" else "Completar serie",
                    tint = if (set.isCompleted) Color.White else colors.inkTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete Set
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Eliminar serie",
                    tint = colors.inkTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Subtle Quick Reference + 1-Tap Stepper Strip (±2.5 kg / ±1 rep)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (previousSet != null) {
                Text(
                    text = "Ant: ${WeightFormatter.formatCompactWeight(previousSet.weightKg, unit, false)}×${previousSet.reps}",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = colors.inkTertiary
                )
            } else {
                Text(
                    text = "Ajuste rápido",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkTertiary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                QuickNudgeChip("-${unit.stepLabel}") {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val nextKg = WeightFormatter.stepWeightKg(currentLocalWeightKg(), -1, unit)
                    val nextDisp = WeightFormatter.kgToDisplay(nextKg, unit)
                    weightText = WeightFormatter.formatNumber(nextDisp)
                    onUpdate(set.copy(weightKg = nextKg, reps = currentLocalReps()), false)
                }
                QuickNudgeChip("+${unit.stepLabel}") {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val nextKg = WeightFormatter.stepWeightKg(currentLocalWeightKg(), +1, unit).coerceAtMost(2000.0)
                    val nextDisp = WeightFormatter.kgToDisplay(nextKg, unit)
                    weightText = WeightFormatter.formatNumber(nextDisp)
                    onUpdate(set.copy(weightKg = nextKg, reps = currentLocalReps()), false)
                }
                QuickNudgeChip("-1r") {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val nextReps = (currentLocalReps() - 1).coerceAtLeast(0)
                    repsText = if (nextReps > 0) nextReps.toString() else ""
                    val stillCompleted = set.isCompleted && nextReps > 0
                    onUpdate(
                        set.copy(
                            weightKg = currentLocalWeightKg(),
                            reps = nextReps,
                            isCompleted = stillCompleted
                        ),
                        false
                    )
                }
                QuickNudgeChip("+1r") {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val nextReps = (currentLocalReps() + 1).coerceAtMost(200)
                    repsText = nextReps.toString()
                    onUpdate(set.copy(weightKg = currentLocalWeightKg(), reps = nextReps), false)
                }
            }
        }
    }
}

@Composable
private fun QuickNudgeChip(label: String, onClick: () -> Unit) {
    val colors = KovaTheme.colors
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.tabular().copy(fontWeight = FontWeight.Medium),
            color = colors.inkSecondary,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummarySheet(
    summary: FinishedWorkoutSummary,
    unit: WeightUnit,
    onDismiss: () -> Unit
) {
    val colors = KovaTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (summary.isDayGoalMet) colors.successSoft else colors.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (summary.newPrExerciseNames.isNotEmpty()) Icons.Rounded.EmojiEvents else Icons.Rounded.Check,
                    contentDescription = null,
                    tint = if (summary.isDayGoalMet) colors.success else colors.accentText,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (summary.isDayGoalMet) "¡Sesión completada!" else "Entrenamiento guardado",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.inkPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${summary.routineName} · ${WeightFormatter.formatDuration(summary.durationSeconds)}",
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                    color = colors.inkSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatTile(
                    label = "Ejercicios",
                    value = "${summary.completedExercises}/${summary.targetExercises}",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatTile(
                    label = "Series",
                    value = "${summary.totalSets}",
                    modifier = Modifier.weight(1f)
                )
                SummaryStatTile(
                    label = "Volumen",
                    value = WeightFormatter.formatVolume(summary.totalVolumeKg, unit),
                    modifier = Modifier.weight(1f)
                )
            }

            if (summary.newPrExerciseNames.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.successSoft,
                    border = BorderStroke(1.dp, colors.successBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NUEVOS RÉCORDS PERSONALES (PR)",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.success
                        )
                        Text(
                            text = summary.newPrExerciseNames.joinToString(" · "),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.inkPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            KovaPrimaryButton(
                text = "Listo",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SummaryStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = KovaTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceTonal
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                color = colors.inkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary
            )
        }
    }
}
