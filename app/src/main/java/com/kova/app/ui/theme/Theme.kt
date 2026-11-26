package com.kova.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kova.app.domain.model.AccentPalette
import com.kova.app.domain.model.ThemeMode

val LocalKovaColors = staticCompositionLocalOf {
    buildKovaColors(isDark = false, palette = AccentPalette.VOLT)
}

object KovaTheme {
    val colors: KovaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalKovaColors.current
}

private val KovaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KovaTheme(
    themeMode: ThemeMode,
    accentPalette: AccentPalette,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val target = buildKovaColors(isDark = isDark, palette = accentPalette)
    val animSpec = tween<Color>(durationMillis = 280)

    val animated = target.copy(
        canvas = animateColorAsState(target.canvas, animSpec, label = "canvas").value,
        surface = animateColorAsState(target.surface, animSpec, label = "surface").value,
        surfaceTonal = animateColorAsState(target.surfaceTonal, animSpec, label = "surfaceTonal").value,
        surfaceElevated = animateColorAsState(target.surfaceElevated, animSpec, label = "surfaceElevated").value,
        border = animateColorAsState(target.border, animSpec, label = "border").value,
        inkPrimary = animateColorAsState(target.inkPrimary, animSpec, label = "inkPrimary").value,
        inkSecondary = animateColorAsState(target.inkSecondary, animSpec, label = "inkSecondary").value,
        accent = animateColorAsState(target.accent, animSpec, label = "accent").value,
        onAccent = animateColorAsState(target.onAccent, animSpec, label = "onAccent").value,
        accentSoft = animateColorAsState(target.accentSoft, animSpec, label = "accentSoft").value,
        accentText = animateColorAsState(target.accentText, animSpec, label = "accentText").value
    )

    val materialScheme = if (isDark) {
        darkColorScheme(
            primary = animated.accent,
            onPrimary = animated.onAccent,
            primaryContainer = animated.accentSoft,
            onPrimaryContainer = animated.accentText,
            secondary = animated.surfaceTonal,
            onSecondary = animated.inkPrimary,
            background = animated.canvas,
            onBackground = animated.inkPrimary,
            surface = animated.surface,
            onSurface = animated.inkPrimary,
            surfaceVariant = animated.surfaceTonal,
            onSurfaceVariant = animated.inkSecondary,
            outline = animated.border,
            outlineVariant = animated.borderSubtle,
            error = animated.danger,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = animated.accent,
            onPrimary = animated.onAccent,
            primaryContainer = animated.accentSoft,
            onPrimaryContainer = animated.accentText,
            secondary = animated.surfaceTonal,
            onSecondary = animated.inkPrimary,
            background = animated.canvas,
            onBackground = animated.inkPrimary,
            surface = animated.surface,
            onSurface = animated.inkPrimary,
            surfaceVariant = animated.surfaceTonal,
            onSurfaceVariant = animated.inkSecondary,
            outline = animated.border,
            outlineVariant = animated.borderSubtle,
            error = animated.danger,
            onError = Color.White
        )
    }

    CompositionLocalProvider(LocalKovaColors provides animated) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = KovaTypography,
            shapes = KovaShapes,
            content = content
        )
    }
}
