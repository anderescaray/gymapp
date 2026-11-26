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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kova.app.domain.model.Exercise
import com.kova.app.domain.model.ExerciseAnalytics
import com.kova.app.domain.model.ProgressTrend
import com.kova.app.domain.model.RoutineDay
import com.kova.app.domain.model.RoutineWithExercises
import com.kova.app.ui.components.ExerciseEditorSheet
import com.kova.app.ui.components.KovaCard
import com.kova.app.ui.components.KovaPrimaryButton
import com.kova.app.ui.components.KovaSecondaryButton
import com.kova.app.ui.components.KovaSectionHeader
import com.kova.app.ui.components.KovaStatusPill
import com.kova.app.ui.components.KovaStepper
import com.kova.app.ui.components.RoutineDayEditorSheet
import com.kova.app.ui.components.parseHexColor
import com.kova.app.ui.theme.KovaTheme
import com.kova.app.ui.theme.tabular

@Composable
fun RoutinesScreen(
    routines: List<RoutineWithExercises>,
    hasActiveSession: Boolean,
    onCreateRoutineDay: (name: String, colorHex: String, targetCount: Int) -> Unit,
    onOpenRoutineDetail: (Long) -> Unit,
    onStartWorkout: (Long) -> Unit
) {
    val colors = KovaTheme.colors
    var showCreateSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TU PROGRAMA",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inkSecondary
                    )
                    Text(
                        text = "Rutinas",
                        style = MaterialTheme.typography.displayLarge,
                        color = colors.inkPrimary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = colors.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .clickable { showCreateSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = colors.onAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Nuevo día",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.onAccent
                        )
                    }
                }
            }
        }

        if (routines.isEmpty()) {
            item {
                KovaCard(contentPadding = PaddingValues(24.dp)) {
                    Text(
                        text = "Sin días de rutina todavía",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.inkPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Añade días como Push, Pull o Leg y configura tus ejercicios obligatorios y opcionales.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkSecondary
                    )
                    Spacer(Modifier.height(18.dp))
                    KovaPrimaryButton(
                        text = "Crear primer día",
                        icon = Icons.Rounded.Add,
                        onClick = { showCreateSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            items(routines, key = { it.routine.id }) { wrapper ->
                RoutineSummaryCard(
                    wrapper = wrapper,
                    canQuickStart = !hasActiveSession,
                    onClick = { onOpenRoutineDetail(wrapper.routine.id) },
                    onStart = { onStartWorkout(wrapper.routine.id) }
                )
            }
        }
    }

    if (showCreateSheet) {
        RoutineDayEditorSheet(
            existing = null,
            onDismiss = { showCreateSheet = false },
            onSave = { name, colorHex, target ->
                onCreateRoutineDay(name, colorHex, target)
                showCreateSheet = false
            }
        )
    }
}

@Composable
private fun RoutineSummaryCard(
    wrapper: RoutineWithExercises,
    canQuickStart: Boolean,
    onClick: () -> Unit,
    onStart: () -> Unit
) {
    val colors = KovaTheme.colors
    val routine = wrapper.routine
    val active = wrapper.activeExercises
    val mandatory = wrapper.mandatoryCount
    val optional = wrapper.optionalCount
    val target = maxOf(routine.targetExerciseCount, mandatory)
    val dotColor = parseHexColor(routine.colorHex)

    KovaCard(
        onClick = onClick,
        contentPadding = PaddingValues(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canQuickStart) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = colors.accentSoft,
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .clickable(onClick = onStart)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Empezar",
                                tint = colors.accentText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Entrenar",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.accentText
                            )
                        }
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.inkTertiary
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KovaStatusPill(
                text = if (active.isEmpty()) "Objetivo: $target" else "Objetivo: $target/${active.size}",
                containerColor = colors.surfaceTonal,
                contentColor = colors.inkPrimary
            )
            KovaStatusPill(
                text = "$mandatory oblig.",
                containerColor = colors.accentSoft,
                contentColor = colors.accentText
            )
            if (optional > 0) {
                KovaStatusPill(
                    text = "$optional opc.",
                    containerColor = colors.surfaceTonal,
                    contentColor = colors.inkSecondary
                )
            }
        }

        if (active.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = active.take(4).joinToString(" · ") { it.name } +
                    if (active.size > 4) "  +${active.size - 4}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Toca para añadir ejercicios obligatorios y opcionales",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkTertiary
            )
        }
    }
}

@Composable
fun RoutineDetailScreen(
    wrapper: RoutineWithExercises?,
    defaultRestSeconds: Int,
    hasActiveSession: Boolean,
    isActiveSessionForThisRoutine: Boolean,
    analyticsByExerciseId: Map<Long, ExerciseAnalytics>,
    onBack: () -> Unit,
    onStartWorkout: (Long) -> Unit,
    onUpdateRoutine: (RoutineDay) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onCreateExercise: (routineDayId: Long, name: String, isMandatory: Boolean, restSeconds: Int?) -> Unit,
    onUpdateExercise: (Exercise) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onOpenExerciseDetail: (Long) -> Unit
) {
    val colors = KovaTheme.colors
    if (wrapper == null) {
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

    val routine = wrapper.routine
    val mandatoryExercises = wrapper.activeExercises.filter { it.isMandatory }
    val optionalExercises = wrapper.activeExercises.filterNot { it.isMandatory }
    val target = maxOf(routine.targetExerciseCount, mandatoryExercises.size.coerceAtLeast(1))
    val optionalRequired = (target - mandatoryExercises.size).coerceAtLeast(0)

    var showEditRoutineSheet by remember { mutableStateOf(false) }
    var showDeleteRoutineConfirm by remember { mutableStateOf(false) }
    var showAddExerciseSheet by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<Exercise?>(null) }
    var deletingExercise by remember { mutableStateOf<Exercise?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Navigation Bar
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(routine.colorHex))
                        )
                        Text(
                            text = routine.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.inkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .clickable { showEditRoutineSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Editar día",
                            tint = colors.inkSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (!isActiveSessionForThisRoutine) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.dangerSoft)
                                .clickable { showDeleteRoutineConfirm = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Eliminar día",
                                tint = colors.danger,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Daily Target Control Card
        item {
            KovaCard(contentPadding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Objetivo de ejercicios",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.inkPrimary
                        )
                        Spacer(Modifier.height(3.dp))
                        val explanation = when {
                            wrapper.activeExercises.isEmpty() -> "Añade ejercicios obligatorios u opcionales"
                            wrapper.activeExercises.size < target ->
                                "Tienes ${wrapper.activeExercises.size} ejercicios · añade ${target - wrapper.activeExercises.size} más o baja el objetivo"
                            optionalRequired == 0 -> "${mandatoryExercises.size} obligatorios completan el día"
                            else -> "${mandatoryExercises.size} obligatorios + $optionalRequired de ${optionalExercises.size} opcionales"
                        }
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary
                        )
                    }
                    KovaStepper(
                        value = target,
                        minValue = mandatoryExercises.size.coerceAtLeast(1),
                        maxValue = 20,
                        onValueChange = { next ->
                            onUpdateRoutine(routine.copy(targetExerciseCount = next))
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!hasActiveSession) {
                        KovaPrimaryButton(
                            text = "Empezar ${routine.name}",
                            icon = Icons.Rounded.PlayArrow,
                            onClick = { onStartWorkout(routine.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    KovaSecondaryButton(
                        text = "Añadir ejercicio",
                        icon = Icons.Rounded.Add,
                        onClick = { showAddExerciseSheet = true },
                        modifier = if (hasActiveSession) Modifier.fillMaxWidth() else Modifier
                    )
                }
            }
        }

        // Mandatory Exercises Section
        if (mandatoryExercises.isNotEmpty()) {
            item {
                KovaSectionHeader(
                    title = "Obligatorios (${mandatoryExercises.size})",
                    trailing = {
                        Text(
                            text = "Siempre en cada sesión",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkTertiary
                        )
                    }
                )
            }
            items(mandatoryExercises, key = { it.id }) { ex ->
                RoutineExerciseRowCard(
                    exercise = ex,
                    defaultRestSeconds = defaultRestSeconds,
                    analytics = analyticsByExerciseId[ex.id],
                    onOpenAnalytics = { onOpenExerciseDetail(ex.id) },
                    onEdit = { editingExercise = ex },
                    onDelete = { deletingExercise = ex }
                )
            }
        }

        // Optional Exercises Section
        if (optionalExercises.isNotEmpty()) {
            item {
                KovaSectionHeader(
                    title = "Opcionales (${optionalExercises.size})",
                    trailing = {
                        Text(
                            text = if (optionalRequired > 0) "Elige al menos $optionalRequired" else "Extras",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkTertiary
                        )
                    }
                )
            }
            items(optionalExercises, key = { it.id }) { ex ->
                RoutineExerciseRowCard(
                    exercise = ex,
                    defaultRestSeconds = defaultRestSeconds,
                    analytics = analyticsByExerciseId[ex.id],
                    onOpenAnalytics = { onOpenExerciseDetail(ex.id) },
                    onEdit = { editingExercise = ex },
                    onDelete = { deletingExercise = ex }
                )
            }
        }

        if (wrapper.activeExercises.isEmpty()) {
            item {
                KovaCard(contentPadding = PaddingValues(22.dp)) {
                    Text(
                        text = "Todavía no hay ejercicios en ${routine.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.inkPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Añade tus ejercicios y marca cuáles son obligatorios y cuáles son opcionales.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkSecondary
                    )
                }
            }
        }
    }

    if (showEditRoutineSheet) {
        RoutineDayEditorSheet(
            existing = routine,
            minTargetCount = mandatoryExercises.size.coerceAtLeast(1),
            onDismiss = { showEditRoutineSheet = false },
            onSave = { name, colorHex, newTarget ->
                onUpdateRoutine(routine.copy(name = name, colorHex = colorHex, targetExerciseCount = newTarget))
                showEditRoutineSheet = false
            }
        )
    }

    if (showAddExerciseSheet) {
        ExerciseEditorSheet(
            existing = null,
            defaultRestSeconds = defaultRestSeconds,
            onDismiss = { showAddExerciseSheet = false },
            onSave = { name, mandatory, rest ->
                onCreateExercise(routine.id, name, mandatory, rest)
                showAddExerciseSheet = false
            }
        )
    }

    editingExercise?.let { ex ->
        ExerciseEditorSheet(
            existing = ex,
            defaultRestSeconds = defaultRestSeconds,
            onDismiss = { editingExercise = null },
            onSave = { name, mandatory, rest ->
                onUpdateExercise(
                    ex.copy(
                        name = name,
                        isMandatory = mandatory,
                        isBodyweight = false,
                        notes = "",
                        defaultRestSeconds = rest
                    )
                )
                editingExercise = null
            }
        )
    }

    if (showDeleteRoutineConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteRoutineConfirm = false },
            containerColor = colors.surface,
            title = { Text("¿Eliminar ${routine.name}?", color = colors.inkPrimary) },
            text = {
                Text(
                    "Se quitará de tus rutinas activas, pero tus entrenamientos pasados seguirán intactos en el calendario.",
                    color = colors.inkSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteRoutineConfirm = false
                    onDeleteRoutine(routine.id)
                    onBack()
                }) {
                    Text("Eliminar", color = colors.danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRoutineConfirm = false }) {
                    Text("Cancelar", color = colors.inkSecondary)
                }
            }
        )
    }

    deletingExercise?.let { ex ->
        AlertDialog(
            onDismissRequest = { deletingExercise = null },
            containerColor = colors.surface,
            title = { Text("¿Quitar ${ex.name}?", color = colors.inkPrimary) },
            text = {
                Text(
                    "Se ocultará de esta rutina. Los registros pasados en tu calendario no se borrarán.",
                    color = colors.inkSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteExercise(ex.id)
                    deletingExercise = null
                }) {
                    Text("Quitar", color = colors.danger, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingExercise = null }) {
                    Text("Cancelar", color = colors.inkSecondary)
                }
            }
        )
    }
}

@Composable
private fun RoutineExerciseRowCard(
    exercise: Exercise,
    defaultRestSeconds: Int,
    analytics: ExerciseAnalytics?,
    onOpenAnalytics: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = KovaTheme.colors
    val restSeconds = exercise.defaultRestSeconds ?: defaultRestSeconds
    KovaCard(
        onClick = onOpenAnalytics,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.inkPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KovaStatusPill(
                        text = "${restSeconds}s",
                        containerColor = colors.surfaceTonal,
                        contentColor = colors.inkSecondary
                    )
                    if (analytics != null && analytics.points.isNotEmpty()) {
                        val (pillBg, pillFg) = when (analytics.trend) {
                            ProgressTrend.NEW_PR -> colors.successSoft to colors.success
                            ProgressTrend.IMPROVING -> colors.accentSoft to colors.accentText
                            ProgressTrend.PLATEAU -> colors.warningSoft to colors.warning
                            ProgressTrend.DIP -> colors.surfaceTonal to colors.inkSecondary
                            ProgressTrend.BUILDING_HISTORY -> colors.surfaceTonal to colors.inkSecondary
                        }
                        KovaStatusPill(
                            text = analytics.trend.badgeLabel,
                            containerColor = pillBg,
                            contentColor = pillFg
                        )
                        Text(
                            text = analytics.points.last().bestSetDescription,
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = colors.inkSecondary
                        )
                    } else {
                        Text(
                            text = "Sin registros aún",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkTertiary
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Editar",
                        tint = colors.inkTertiary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Eliminar",
                        tint = colors.inkTertiary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Ver gráfica",
                    tint = colors.inkTertiary
                )
            }
        }
    }
}
