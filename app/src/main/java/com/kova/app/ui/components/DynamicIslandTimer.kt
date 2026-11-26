package com.kova.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kova.app.domain.logic.WeightFormatter
import com.kova.app.ui.state.RestTimerState
import com.kova.app.ui.theme.KovaTheme
import com.kova.app.ui.theme.tabular

@Composable
fun DynamicIslandRestTimer(
    state: RestTimerState,
    accentColor: Color,
    onAdjust: (Int) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onToggleExpand: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KovaTheme.colors
    val haptics = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
        ) + fadeOut(),
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
    ) {
        val ringAccent by animateColorAsState(
            targetValue = if (state.isFinished) Color(0xFF34D399) else accentColor,
            label = "islandRing"
        )
        val borderTint by animateColorAsState(
            targetValue = if (state.isFinished) Color(0xFF34D399).copy(alpha = 0.65f) else Color(0xFF2B303B),
            label = "islandBorder"
        )

        Surface(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                )
                .clip(RoundedCornerShape(if (state.isExpanded) 28.dp else 100.dp))
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (state.isFinished) onDismiss() else onToggleExpand()
                },
            shape = RoundedCornerShape(if (state.isExpanded) 28.dp else 100.dp),
            color = colors.islandBg,
            border = BorderStroke(1.dp, borderTint),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = if (state.isExpanded) 18.dp else 14.dp,
                    vertical = if (state.isExpanded) 14.dp else 9.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.isFinished) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "¡Descanso listo!",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.islandInk
                            )
                            if (state.exerciseName.isNotBlank()) {
                                Text(
                                    text = "Siguiente serie · ${state.exerciseName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.islandInkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        ProgressRing(
                            progress = state.progress,
                            ringColor = ringAccent,
                            trackColor = colors.islandSurface,
                            size = 30.dp,
                            strokeWidth = 3.5.dp
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = ringAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = WeightFormatter.formatDuration(state.remainingSeconds.toLong()),
                                style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                                color = colors.islandInk
                            )
                            if (state.exerciseName.isNotBlank()) {
                                Text(
                                    text = state.exerciseName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.islandInkMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IslandMiniChip(label = "-15s") {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAdjust(-15)
                        }
                        IslandMiniChip(label = "+15s") {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAdjust(15)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.islandSurface)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar temporizador",
                            tint = colors.islandInkMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                if (state.isExpanded && !state.isFinished) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(60, 90, 120, 150, 180).forEach { preset ->
                            val active = state.totalSeconds == preset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(if (active) ringAccent else colors.islandSurface)
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectPreset(preset)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${preset}s",
                                    style = MaterialTheme.typography.labelMedium.tabular(),
                                    color = if (active) Color(0xFF0E1203) else colors.islandInk
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(100.dp))
                                .background(colors.islandSurface)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onTogglePause()
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (state.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = colors.islandInk,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (state.isRunning) "Pausar" else "Reanudar",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.islandInk
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(100.dp))
                                .background(colors.islandSurface)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Saltar descanso",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.islandInkMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandMiniChip(label: String, onClick: () -> Unit) {
    val colors = KovaTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(colors.islandSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.tabular(),
            color = colors.islandInk
        )
    }
}
