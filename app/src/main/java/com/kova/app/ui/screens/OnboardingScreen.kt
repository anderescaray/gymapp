package com.kova.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kova.app.domain.model.RoutineColorPresets
import com.kova.app.domain.model.RoutineWithExercises
import com.kova.app.ui.components.KovaCard
import com.kova.app.ui.components.KovaLogoMark
import com.kova.app.ui.components.KovaPrimaryButton
import com.kova.app.ui.components.KovaSegmentedControl
import com.kova.app.ui.components.KovaStatusPill
import com.kova.app.ui.components.KovaStepper
import com.kova.app.ui.components.parseHexColor
import com.kova.app.ui.theme.KovaTheme
import com.kova.app.ui.theme.tabular

@Composable
fun OnboardingScreen(
    initialWeeklyGoal: Int,
    accentColor: androidx.compose.ui.graphics.Color,
    routines: List<RoutineWithExercises>,
    onCreateRoutineDay: (name: String, colorHex: String, targetCount: Int) -> Unit,
    onUpdateRoutineTarget: (RoutineWithExercises, Int) -> Unit,
    onArchiveRoutineDay: (Long) -> Unit,
    onCreateExercise: (routineDayId: Long, name: String, isMandatory: Boolean) -> Unit,
    onArchiveExercise: (Long) -> Unit,
    onFinishOnboarding: (weeklyGoal: Int) -> Unit
) {
    val colors = KovaTheme.colors
    var step by rememberSaveable { mutableIntStateOf(0) }
    var weeklyGoal by rememberSaveable { mutableIntStateOf(initialWeeklyGoal.coerceIn(1, 7)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top Progress Indicator Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (step > 0) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .clickable { step-- },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Atrás",
                        tint = colors.inkPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(4) { idx ->
                    val filled = idx <= step
                    val barColor by animateColorAsState(
                        targetValue = if (filled) colors.accent else colors.border,
                        label = "stepBar"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(barColor)
                    )
                }
            }
            Text(
                text = "${step + 1}/4",
                style = MaterialTheme.typography.labelMedium.tabular(),
                color = colors.inkTertiary
            )
        }

        // Animated Step Content
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn())
                    .togetherWith(slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "onboardingSteps"
        ) { currentStep ->
            when (currentStep) {
                0 -> OnboardingWeeklyGoalStep(
                    weeklyGoal = weeklyGoal,
                    accentColor = accentColor,
                    onWeeklyGoalChange = { weeklyGoal = it }
                )
                1 -> OnboardingRoutineDaysStep(
                    routines = routines,
                    onAddRoutine = onCreateRoutineDay,
                    onRemoveRoutine = onArchiveRoutineDay
                )
                2 -> OnboardingExercisesStep(
                    routines = routines,
                    onUpdateTarget = onUpdateRoutineTarget,
                    onAddExercise = onCreateExercise,
                    onRemoveExercise = onArchiveExercise
                )
                else -> OnboardingReadyStep(
                    weeklyGoal = weeklyGoal,
                    routines = routines
                )
            }
        }

        // Bottom CTA Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (step) {
                0 -> KovaPrimaryButton(
                    text = "Continuar",
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    onClick = { step = 1 },
                    modifier = Modifier.fillMaxWidth()
                )
                1 -> {
                    val canContinue = routines.isNotEmpty()
                    KovaPrimaryButton(
                        text = if (canContinue) "Configurar ejercicios" else "Añade al menos un día",
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        enabled = canContinue,
                        onClick = { step = 2 },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                2 -> KovaPrimaryButton(
                    text = "Siguiente paso",
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    onClick = { step = 3 },
                    modifier = Modifier.fillMaxWidth()
                )
                else -> KovaPrimaryButton(
                    text = "Empezar a entrenar",
                    icon = Icons.Rounded.Check,
                    onClick = { onFinishOnboarding(weeklyGoal) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun OnboardingWeeklyGoalStep(
    weeklyGoal: Int,
    accentColor: androidx.compose.ui.graphics.Color,
    onWeeklyGoalChange: (Int) -> Unit
) {
    val colors = KovaTheme.colors
    val haptics = LocalHapticFeedback.current
    val weekLabels = listOf("L", "M", "X", "J", "V", "S", "D")
    val sampleDays = remember(weeklyGoal) {
        when (weeklyGoal) {
            1 -> setOf(0)
            2 -> setOf(0, 3)
            3 -> setOf(0, 2, 4)
            4 -> setOf(0, 1, 3, 4)
            5 -> setOf(0, 1, 2, 4, 5)
            6 -> setOf(0, 1, 2, 3, 4, 5)
            else -> setOf(0, 1, 2, 3, 4, 5, 6)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KovaLogoMark(size = 46.dp, accentCollarColor = accentColor)
            Column {
                Text(
                    text = "KOVA",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accentText
                )
                Text(
                    text = "Entreno y progreso",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkSecondary
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "¿Cuántos días quieres ir al gym a la semana?",
                style = MaterialTheme.typography.displaySmall,
                color = colors.inkPrimary
            )
            Text(
                text = "Tu calendario marcará en verde las semanas en las que llegues al objetivo y en rojo si te quedas por debajo.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.inkSecondary
            )
        }

        // Tactile Big Goal Selector Card
        KovaCard(
            contentPadding = PaddingValues(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$weeklyGoal ${if (weeklyGoal == 1) "día" else "días"}",
                        style = MaterialTheme.typography.displayLarge.tabular(),
                        color = colors.inkPrimary
                    )
                    Text(
                        text = "Objetivo semanal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkSecondary
                    )
                }
                KovaStepper(
                    value = weeklyGoal,
                    minValue = 1,
                    maxValue = 7,
                    onValueChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onWeeklyGoalChange(it)
                    }
                )
            }

            Spacer(Modifier.height(22.dp))

            // Quick selectable day count pills 1..7
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..7).forEach { count ->
                    val selected = count == weeklyGoal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) colors.accent else colors.surfaceTonal)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onWeeklyGoalChange(count)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.titleSmall.tabular().copy(fontWeight = FontWeight.Bold),
                            color = if (selected) colors.onAccent else colors.inkSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // Live mini week preview
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.successSoft,
                border = BorderStroke(1.dp, colors.successBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Así se verá una semana cumplida",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.success
                        )
                        Text(
                            text = "$weeklyGoal/$weeklyGoal ✓",
                            style = MaterialTheme.typography.labelMedium.tabular(),
                            color = colors.success
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekLabels.forEachIndexed { idx, label ->
                            val trained = idx in sampleDays
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (trained) colors.success else colors.surface.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (trained) androidx.compose.ui.graphics.Color.White else colors.inkTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingRoutineDaysStep(
    routines: List<RoutineWithExercises>,
    onAddRoutine: (name: String, colorHex: String, targetCount: Int) -> Unit,
    onRemoveRoutine: (Long) -> Unit
) {
    val colors = KovaTheme.colors
    var customName by remember { mutableStateOf("") }
    var pendingAdds by remember { mutableStateOf(emptySet<String>()) }
    val suggestions = listOf("Push", "Pull", "Leg", "Torso", "Pierna", "Full Body")
    val existingNames = routines.map { it.routine.name.lowercase() }.toSet()
    val usedColors = routines.map { it.routine.colorHex.lowercase() }.toSet()

    LaunchedEffect(routines.size) {
        pendingAdds = emptySet()
    }

    fun nextAvailableColor(): String {
        return RoutineColorPresets.hexPalette.firstOrNull { it.lowercase() !in usedColors }
            ?: RoutineColorPresets.hexPalette[routines.size % RoutineColorPresets.hexPalette.size]
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Crea tus días de rutina",
                style = MaterialTheme.typography.displaySmall,
                color = colors.inkPrimary
            )
            Text(
                text = "Añade los bloques que entrenas (por ejemplo Push, Pull y Leg). Luego rotarás entre ellos con total libertad.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.inkSecondary
            )
        }

        // Quick tap chips
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "AÑADIR RÁPIDO",
                style = MaterialTheme.typography.labelSmall,
                color = colors.inkSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { suggestion ->
                    val key = suggestion.lowercase()
                    val alreadyAdded = key in existingNames || key in pendingAdds
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (alreadyAdded) colors.accentSoft else colors.surface,
                        border = BorderStroke(1.dp, if (alreadyAdded) colors.accentText.copy(alpha = 0.4f) else colors.border),
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .clickable(enabled = !alreadyAdded) {
                                pendingAdds = pendingAdds + key
                                onAddRoutine(suggestion, nextAvailableColor(), 5)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (alreadyAdded) Icons.Rounded.Check else Icons.Rounded.Add,
                                contentDescription = null,
                                tint = if (alreadyAdded) colors.accentText else colors.inkPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (alreadyAdded) colors.accentText else colors.inkPrimary
                            )
                        }
                    }
                }
            }
        }

        // Custom day input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = customName,
                onValueChange = { customName = it },
                placeholder = { Text("O escribe tu propio día...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface
                ),
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (customName.trim().isNotEmpty()) colors.accent else colors.surfaceTonal,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = customName.trim().isNotEmpty()) {
                        onAddRoutine(customName.trim(), nextAvailableColor(), 5)
                        customName = ""
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Añadir día",
                        tint = if (customName.trim().isNotEmpty()) colors.onAccent else colors.inkTertiary
                    )
                }
            }
        }

        // Current created days
        if (routines.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "TUS DÍAS (${routines.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkSecondary
                )
                routines.forEach { item ->
                    KovaCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(parseHexColor(item.routine.colorHex))
                                )
                                Column {
                                    Text(
                                        text = item.routine.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.inkPrimary
                                    )
                                    Text(
                                        text = "Objetivo: ${item.routine.targetExerciseCount} ejercicios",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.inkSecondary
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceTonal)
                                    .clickable { onRemoveRoutine(item.routine.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Quitar",
                                    tint = colors.inkSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OnboardingExercisesStep(
    routines: List<RoutineWithExercises>,
    onUpdateTarget: (RoutineWithExercises, Int) -> Unit,
    onAddExercise: (routineDayId: Long, name: String, isMandatory: Boolean) -> Unit,
    onRemoveExercise: (Long) -> Unit
) {
    val colors = KovaTheme.colors
    var selectedRoutineId by rememberSaveable { mutableLongStateOf(routines.firstOrNull()?.routine?.id ?: -1L) }

    LaunchedEffect(routines) {
        if (routines.none { it.routine.id == selectedRoutineId }) {
            selectedRoutineId = routines.firstOrNull()?.routine?.id ?: -1L
        }
    }

    val current = routines.firstOrNull { it.routine.id == selectedRoutineId } ?: routines.firstOrNull()
    var exerciseName by remember { mutableStateOf("") }
    var isMandatory by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Ejercicios y objetivo diario",
                style = MaterialTheme.typography.displaySmall,
                color = colors.inkPrimary
            )
            Text(
                text = "Dentro de cada día marca los ejercicios obligatorios (siempre) y los opcionales (eliges para llegar al objetivo). Puedes ampliarlo después cuando quieras.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkSecondary
            )
        }

        // Routine day selector pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            routines.forEach { item ->
                val selected = item.routine.id == selectedRoutineId
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (selected) colors.accent else colors.surface,
                    border = BorderStroke(1.dp, if (selected) colors.accent else colors.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .clickable { selectedRoutineId = item.routine.id }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) colors.onAccent else parseHexColor(item.routine.colorHex)
                                )
                        )
                        Text(
                            text = "${item.routine.name} (${item.activeExercises.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) colors.onAccent else colors.inkPrimary
                        )
                    }
                }
            }
        }

        if (current != null) {
            val mandatoryCount = current.mandatoryCount
            val optionalCount = current.optionalCount
            val target = maxOf(current.routine.targetExerciseCount, mandatoryCount.coerceAtLeast(1))
            val optionalNeeded = (target - mandatoryCount).coerceAtLeast(0)

            // Target stepper card
            KovaCard(contentPadding = PaddingValues(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Objetivo para ${current.routine.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.inkPrimary
                        )
                        val ruleSummary = when {
                            current.activeExercises.isEmpty() -> "Añade tus ejercicios abajo"
                            current.activeExercises.size < target ->
                                "Tienes ${current.activeExercises.size} ejercicios (añade ${target - current.activeExercises.size} más o baja el objetivo)"
                            optionalNeeded == 0 -> "$mandatoryCount obligatorios para cerrar el día"
                            else -> "$mandatoryCount oblig. + elige $optionalNeeded de $optionalCount opc."
                        }
                        Text(
                            text = ruleSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary
                        )
                    }
                    KovaStepper(
                        value = target,
                        minValue = mandatoryCount.coerceAtLeast(1),
                        maxValue = 20,
                        onValueChange = { onUpdateTarget(current, it) }
                    )
                }
            }

            // Inline add exercise box
            KovaCard(contentPadding = PaddingValues(16.dp)) {
                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    placeholder = { Text("Ej. Press banca, Dominadas...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                KovaSegmentedControl(
                    options = listOf(true, false),
                    selectedOption = isMandatory,
                    labelProvider = { if (it) "Obligatorio" else "Opcional" },
                    onSelect = { isMandatory = it }
                )
                Spacer(Modifier.height(10.dp))
                KovaPrimaryButton(
                    text = "Añadir",
                    icon = Icons.Rounded.Add,
                    enabled = exerciseName.trim().isNotEmpty(),
                    onClick = {
                        onAddExercise(current.routine.id, exerciseName.trim(), isMandatory)
                        exerciseName = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Added exercises list
            if (current.activeExercises.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    current.activeExercises.forEach { ex ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ex.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.inkPrimary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        KovaStatusPill(
                                            text = if (ex.isMandatory) "Obligatorio" else "Opcional",
                                            containerColor = if (ex.isMandatory) colors.accentSoft else colors.surfaceTonal,
                                            contentColor = if (ex.isMandatory) colors.accentText else colors.inkSecondary
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colors.surfaceTonal)
                                        .clickable { onRemoveExercise(ex.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Quitar ejercicio",
                                        tint = colors.inkSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OnboardingReadyStep(
    weeklyGoal: Int,
    routines: List<RoutineWithExercises>
) {
    val colors = KovaTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KovaStatusPill(
                text = "Todo listo · $weeklyGoal días/semana · ${routines.size} rutinas",
                containerColor = colors.successSoft,
                contentColor = colors.success,
                borderColor = colors.successBorder
            )
            Text(
                text = "Tus pesos se apuntan entrenando",
                style = MaterialTheme.typography.displaySmall,
                color = colors.inkPrimary
            )
            Text(
                text = "No tienes que meter tus marcas ahora. Empieza tu primera sesión en el gimnasio y Kova construirá tus gráficas automáticamente.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.inkSecondary
            )
        }

        OnboardingGuideRow(
            icon = Icons.Rounded.FitnessCenter,
            title = "Series en un toque",
            subtitle = "Apunta kilos y repeticiones sobre la marcha. A partir de la segunda sesión verás tu «Última vez» como referencia."
        )
        OnboardingGuideRow(
            icon = Icons.Rounded.Timer,
            title = "Descanso flotante con vibración",
            subtitle = "Al marcar una serie, la cuenta atrás sigue visible sobre otras apps y vibra al terminar."
        )
        OnboardingGuideRow(
            icon = Icons.Rounded.AutoGraph,
            title = "Progreso, PR y estancamiento",
            subtitle = "Dentro de cada ejercicio verás tu curva de 1RM, peso máximo, volumen y si llevas varias sesiones estancado."
        )
    }
}

@Composable
private fun OnboardingGuideRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val colors = KovaTheme.colors
    KovaCard(contentPadding = PaddingValues(16.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.inkPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkSecondary
                )
            }
        }
    }
}
