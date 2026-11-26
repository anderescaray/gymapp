package com.kova.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kova.app.domain.model.Exercise
import com.kova.app.domain.model.RoutineColorPresets
import com.kova.app.domain.model.RoutineDay
import com.kova.app.ui.theme.KovaTheme
import com.kova.app.ui.theme.tabular

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDayEditorSheet(
    existing: RoutineDay?,
    minTargetCount: Int = 1,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String, targetCount: Int) -> Unit
) {
    val colors = KovaTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var colorHex by remember(existing) { mutableStateOf(existing?.colorHex ?: RoutineColorPresets.hexPalette.first()) }
    var targetCount by remember(existing, minTargetCount) {
        mutableIntStateOf((existing?.targetExerciseCount ?: 5).coerceAtLeast(minTargetCount.coerceAtLeast(1)))
    }

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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (existing == null) "Nuevo día" else "Editar día",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.inkPrimary
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                placeholder = { Text("Ej. Push, Pull, Leg...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border,
                    focusedLabelColor = colors.accentText
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Objetivo de ejercicios",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.inkPrimary
                )
                KovaStepper(
                    value = targetCount,
                    minValue = minTargetCount.coerceAtLeast(1),
                    maxValue = 20,
                    onValueChange = { targetCount = it }
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Color",
                style = MaterialTheme.typography.titleSmall,
                color = colors.inkPrimary
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RoutineColorPresets.hexPalette.forEach { hex ->
                    val dotColor = parseHexColor(hex)
                    val selected = hex.equals(colorHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .then(
                                if (selected) Modifier.border(2.5.dp, colors.inkPrimary, CircleShape)
                                else Modifier
                            )
                            .clickable { colorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color(0xFF111318),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            KovaPrimaryButton(
                text = if (existing == null) "Crear día" else "Guardar",
                enabled = name.trim().isNotEmpty(),
                onClick = { onSave(name.trim(), colorHex, targetCount) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseEditorSheet(
    existing: Exercise?,
    defaultRestSeconds: Int,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        isMandatory: Boolean,
        restSeconds: Int?
    ) -> Unit
) {
    val colors = KovaTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val restPresets = remember { listOf(60, 90, 120, 150, 180) }

    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var isMandatory by remember(existing) { mutableStateOf(existing?.isMandatory ?: true) }
    var restSeconds by remember(existing, defaultRestSeconds) {
        mutableIntStateOf(existing?.defaultRestSeconds ?: defaultRestSeconds)
    }
    var hasCustomRest by remember(existing) {
        mutableStateOf(existing?.defaultRestSeconds != null)
    }

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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (existing == null) "Nuevo ejercicio" else "Editar ejercicio",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.inkPrimary
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                placeholder = { Text("Ej. Press banca") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border,
                    focusedLabelColor = colors.accentText
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            KovaSegmentedControl(
                options = listOf(true, false),
                selectedOption = isMandatory,
                labelProvider = { if (it) "Obligatorio" else "Opcional" },
                onSelect = { isMandatory = it }
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Descanso",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.inkPrimary
                )
                Text(
                    text = "${restSeconds}s",
                    style = MaterialTheme.typography.labelLarge.tabular(),
                    color = colors.accentText
                )
            }
            Spacer(Modifier.height(8.dp))
            KovaSegmentedControl(
                options = restPresets,
                selectedOption = restSeconds.takeIf { it in restPresets },
                labelProvider = { "${it}s" },
                onSelect = {
                    restSeconds = it
                    hasCustomRest = true
                }
            )

            Spacer(Modifier.height(24.dp))

            KovaPrimaryButton(
                text = if (existing == null) "Añadir ejercicio" else "Guardar",
                enabled = name.trim().isNotEmpty(),
                onClick = {
                    onSave(
                        name.trim(),
                        isMandatory,
                        if (hasCustomRest) restSeconds else null
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
