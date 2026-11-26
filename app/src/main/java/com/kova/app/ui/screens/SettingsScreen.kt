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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kova.app.domain.model.AccentPalette
import com.kova.app.domain.model.ThemeMode
import com.kova.app.domain.model.UserPreferences
import com.kova.app.domain.model.WeightUnit
import com.kova.app.ui.components.KovaCard
import com.kova.app.ui.components.KovaSecondaryButton
import com.kova.app.ui.components.KovaSectionHeader
import com.kova.app.ui.components.KovaSegmentedControl
import com.kova.app.ui.components.KovaStatusPill
import com.kova.app.ui.components.KovaStepper
import com.kova.app.ui.theme.KovaTheme
import com.kova.app.ui.theme.tabular
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    canDrawOverlays: Boolean,
    onUpdateWeeklyGoal: (Int) -> Unit,
    onUpdateWeightUnit: (WeightUnit) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateAccentPalette: (AccentPalette) -> Unit,
    onUpdateDefaultRestSeconds: (Int) -> Unit,
    onUpdateAutoStartRestTimer: (Boolean) -> Unit,
    onUpdateSoundAndHaptics: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onGoogleDriveClick: () -> Unit,
    onReopenOnboarding: () -> Unit,
    onClearAllData: () -> Unit
) {
    val colors = KovaTheme.colors
    var confirmClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "PREFERENCIAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.inkSecondary
                )
                Text(
                    text = "Ajustes",
                    style = MaterialTheme.typography.displayLarge,
                    color = colors.inkPrimary
                )
            }
        }

        // Training Preferences
        item { KovaSectionHeader(title = "Entrenamiento") }
        item {
            KovaCard(contentPadding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Objetivo semanal", style = MaterialTheme.typography.titleMedium, color = colors.inkPrimary)
                        Text("Días mínimos de lunes a domingo", style = MaterialTheme.typography.bodySmall, color = colors.inkSecondary)
                    }
                    KovaStepper(
                        value = preferences.weeklyGoalDays,
                        minValue = 1,
                        maxValue = 7,
                        onValueChange = onUpdateWeeklyGoal
                    )
                }

                SettingsDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Unidades de peso", style = MaterialTheme.typography.titleMedium, color = colors.inkPrimary)
                    KovaSegmentedControl(
                        options = WeightUnit.entries,
                        selectedOption = preferences.weightUnit,
                        labelProvider = { if (it == WeightUnit.KG) "Kilogramos (kg)" else "Libras (lb)" },
                        onSelect = onUpdateWeightUnit
                    )
                }

                SettingsDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Descanso por defecto", style = MaterialTheme.typography.titleMedium, color = colors.inkPrimary)
                        Text(
                            text = "${preferences.defaultRestSeconds}s",
                            style = MaterialTheme.typography.labelLarge.tabular(),
                            color = colors.accentText
                        )
                    }
                    val restPresets = listOf(60, 90, 120, 150, 180)
                    KovaSegmentedControl(
                        options = restPresets,
                        selectedOption = preferences.defaultRestSeconds.takeIf { it in restPresets },
                        labelProvider = { "${it}s" },
                        onSelect = onUpdateDefaultRestSeconds
                    )
                }

                SettingsDivider()

                SettingsSwitchRow(
                    title = "Descanso automático",
                    subtitle = "Empieza al marcar cada serie",
                    checked = preferences.autoStartRestTimer,
                    onCheckedChange = onUpdateAutoStartRestTimer
                )

                SettingsDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRequestOverlayPermission),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Descanso flotante", style = MaterialTheme.typography.titleMedium, color = colors.inkPrimary)
                        Text(
                            text = if (canDrawOverlays) "Visible sobre otras apps" else "Actívalo para verlo al cambiar de app",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkSecondary
                        )
                    }
                    KovaStatusPill(
                        text = if (canDrawOverlays) "Activo" else "Activar",
                        containerColor = if (canDrawOverlays) colors.successSoft else colors.accentSoft,
                        contentColor = if (canDrawOverlays) colors.success else colors.accentText
                    )
                }

                SettingsDivider()

                SettingsSwitchRow(
                    title = "Vibración al terminar",
                    subtitle = "Vibra al acabar el descanso, incluso fuera de la app",
                    checked = preferences.soundAndHapticsEnabled,
                    onCheckedChange = onUpdateSoundAndHaptics
                )
            }
        }

        // Visual Identity & Theme
        item { KovaSectionHeader(title = "Apariencia") }
        item {
            KovaCard(contentPadding = PaddingValues(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tema", style = MaterialTheme.typography.titleMedium, color = colors.inkPrimary)
                    KovaSegmentedControl(
                        options = ThemeMode.entries,
                        selectedOption = preferences.themeMode,
                        labelProvider = { it.displayName },
                        onSelect = onUpdateThemeMode
                    )
                }

                SettingsDivider()

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Color acento", style = MaterialTheme.typography.titleMedium, color = colors.inkPrimary)
                        Text(
                            text = preferences.accentPalette.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accentText
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        AccentPalette.entries.chunked(3).forEach { rowPalettes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowPalettes.forEach { palette ->
                                    val selected = palette == preferences.accentPalette
                                    val swatchColor = Color(
                                        if (colors.isDark) palette.previewHexDark else palette.previewHexLight
                                    )
                                    val useDarkCheck = colors.isDark || palette == AccentPalette.ROSE
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { onUpdateAccentPalette(palette) }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                                .then(
                                                    if (selected) Modifier.border(3.dp, colors.inkPrimary, CircleShape)
                                                    else Modifier.border(1.dp, colors.border, CircleShape)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = palette.displayName,
                                                    tint = if (useDarkCheck) Color(0xFF0E1105) else Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = palette.displayName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                            ),
                                            color = if (selected) colors.inkPrimary else colors.inkTertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Local Backup & Future Google Drive Sync
        item { KovaSectionHeader(title = "Copia de seguridad y nube") }
        item {
            KovaCard(contentPadding = PaddingValues(18.dp)) {
                Text(
                    text = "Base de datos local (SQLite)",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.inkPrimary
                )
                Spacer(Modifier.height(4.dp))
                val lastBackupLabel = preferences.lastBackupTimestampMillis?.let { ts ->
                    val fmt = SimpleDateFormat("d MMM yyyy · HH:mm", Locale.forLanguageTag("es-ES"))
                    "Última exportación: ${fmt.format(Date(ts))}"
                } ?: "Tus datos viven 100% en tu teléfono, sin conexión ni cuentas."
                Text(
                    text = lastBackupLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkSecondary
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KovaSecondaryButton(
                        text = "Exportar JSON",
                        icon = Icons.Rounded.Upload,
                        onClick = onExportBackup,
                        modifier = Modifier.weight(1f)
                    )
                    KovaSecondaryButton(
                        text = "Importar",
                        icon = Icons.Rounded.Download,
                        onClick = onImportBackup,
                        modifier = Modifier.weight(1f)
                    )
                }

                SettingsDivider()

                // Future Google Drive Integration Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceTonal,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onGoogleDriveClick)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudQueue,
                                contentDescription = null,
                                tint = colors.accentText,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Google Drive",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.inkPrimary
                                )
                                KovaStatusPill(
                                    text = "Preparado",
                                    containerColor = colors.accentSoft,
                                    contentColor = colors.accentText
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Arquitectura lista (kova-backup-v1.json en appDataFolder) para conectar OAuth cuando quieras sincronizar en la nube.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkSecondary
                            )
                        }
                    }
                }
            }
        }

        // Tutorial & Data
        item { KovaSectionHeader(title = "Datos y guía") }
        item {
            KovaCard(contentPadding = PaddingValues(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    KovaSecondaryButton(
                        text = "Volver a ver el tutorial inicial",
                        icon = Icons.Rounded.Replay,
                        onClick = onReopenOnboarding,
                        modifier = Modifier.fillMaxWidth()
                    )
                    KovaSecondaryButton(
                        text = "Borrar todos los datos",
                        icon = Icons.Rounded.DeleteForever,
                        containerColor = colors.dangerSoft,
                        contentColor = colors.danger,
                        borderColor = colors.dangerBorder,
                        onClick = { confirmClearDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Text(
                text = "Kova v1.0.0 · Kotlin + Jetpack Compose + Room SQLite",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    if (confirmClearDialog) {
        AlertDialog(
            onDismissRequest = { confirmClearDialog = false },
            containerColor = colors.surface,
            title = { Text("¿Borrar todos tus datos?", color = colors.inkPrimary) },
            text = {
                Text(
                    "Se eliminarán todas tus rutinas, ejercicios e historial de entrenamientos en este teléfono. Puedes exportar una copia JSON antes.",
                    color = colors.inkSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearDialog = false
                    onClearAllData()
                }) {
                    Text("Borrar todo", color = colors.danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearDialog = false }) {
                    Text("Cancelar", color = colors.inkSecondary)
                }
            }
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 15.dp)
            .height(1.dp)
            .background(KovaTheme.colors.borderSubtle)
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = KovaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.inkPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.inkSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent
            )
        )
    }
}
