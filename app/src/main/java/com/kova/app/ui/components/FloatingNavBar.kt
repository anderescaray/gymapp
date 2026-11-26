package com.kova.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kova.app.ui.theme.KovaTheme

enum class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "Hoy", Icons.Rounded.Home),
    ROUTINES("routines", "Rutinas", Icons.Rounded.FitnessCenter),
    CALENDAR("calendar", "Calendario", Icons.Rounded.CalendarMonth),
    SETTINGS("settings", "Ajustes", Icons.Rounded.Tune)
}

@Composable
fun FloatingNavBar(
    currentRoute: String?,
    onNavigate: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KovaTheme.colors
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = colors.surface.copy(alpha = if (colors.isDark) 0.94f else 0.96f),
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = if (colors.isDark) 12.dp else 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomTab.entries.forEach { tab ->
                    val selected = currentRoute == tab.route
                    val pillBg by animateColorAsState(
                        targetValue = if (selected) colors.accentSoft else Color.Transparent,
                        label = "navPillBg"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (selected) colors.accentText else colors.inkTertiary,
                        label = "navIconTint"
                    )
                    val textTint by animateColorAsState(
                        targetValue = if (selected) colors.inkPrimary else colors.inkTertiary,
                        label = "navTextTint"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(pillBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!selected) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onNavigate(tab)
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = iconTint,
                            modifier = Modifier.size(21.dp)
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = textTint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
