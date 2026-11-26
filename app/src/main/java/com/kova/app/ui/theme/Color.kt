package com.kova.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.kova.app.domain.model.AccentPalette

data class KovaColors(
    val isDark: Boolean,
    val canvas: Color,
    val surface: Color,
    val surfaceTonal: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderSubtle: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val accentText: Color,
    val success: Color,
    val successSoft: Color,
    val successBorder: Color,
    val danger: Color,
    val dangerSoft: Color,
    val dangerBorder: Color,
    val warning: Color,
    val warningSoft: Color,
    val warningBorder: Color,
    val islandBg: Color,
    val islandSurface: Color,
    val islandInk: Color,
    val islandInkMuted: Color
)

private data class AccentTokens(
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val accentText: Color
)

private fun resolveAccentTokens(palette: AccentPalette, isDark: Boolean): AccentTokens {
    return if (isDark) {
        when (palette) {
            AccentPalette.VOLT -> AccentTokens(
                accent = Color(0xFFC6F635),
                onAccent = Color(0xFF0E1203),
                accentSoft = Color(0xFF1F280B),
                accentText = Color(0xFFC6F635)
            )
            AccentPalette.CORAL -> AccentTokens(
                accent = Color(0xFFFF6B4A),
                onAccent = Color(0xFF1C0803),
                accentSoft = Color(0xFF2D1611),
                accentText = Color(0xFFFF866B)
            )
            AccentPalette.COBALT -> AccentTokens(
                accent = Color(0xFF5C9DFF),
                onAccent = Color(0xFF04142E),
                accentSoft = Color(0xFF12213B),
                accentText = Color(0xFF7CB2FF)
            )
            AccentPalette.EMERALD -> AccentTokens(
                accent = Color(0xFF34D399),
                onAccent = Color(0xFF042217),
                accentSoft = Color(0xFF0F2920),
                accentText = Color(0xFF4ADEA8)
            )
            AccentPalette.AMBER -> AccentTokens(
                accent = Color(0xFFFBBF24),
                onAccent = Color(0xFF231602),
                accentSoft = Color(0xFF2C210B),
                accentText = Color(0xFFFCD34D)
            )
            AccentPalette.ROSE -> AccentTokens(
                accent = Color(0xFFF7A8C4),
                onAccent = Color(0xFF240914),
                accentSoft = Color(0xFF2D1922),
                accentText = Color(0xFFF9B8CF)
            )
        }
    } else {
        when (palette) {
            AccentPalette.VOLT -> AccentTokens(
                accent = Color(0xFF15181C),
                onAccent = Color(0xFFC6F635),
                accentSoft = Color(0xFFEBF7C6),
                accentText = Color(0xFF356B00)
            )
            AccentPalette.CORAL -> AccentTokens(
                accent = Color(0xFFD94426),
                onAccent = Color(0xFFFFFFFF),
                accentSoft = Color(0xFFFCE9E4),
                accentText = Color(0xFFC43A1E)
            )
            AccentPalette.COBALT -> AccentTokens(
                accent = Color(0xFF0A66FF),
                onAccent = Color(0xFFFFFFFF),
                accentSoft = Color(0xFFE6F0FF),
                accentText = Color(0xFF0854D6)
            )
            AccentPalette.EMERALD -> AccentTokens(
                accent = Color(0xFF0E8A5F),
                onAccent = Color(0xFFFFFFFF),
                accentSoft = Color(0xFFE3F7EE),
                accentText = Color(0xFF0A734F)
            )
            AccentPalette.AMBER -> AccentTokens(
                accent = Color(0xFFC26D00),
                onAccent = Color(0xFFFFFFFF),
                accentSoft = Color(0xFFFEF2DE),
                accentText = Color(0xFFA65B00)
            )
            AccentPalette.ROSE -> AccentTokens(
                accent = Color(0xFFF6A6C1),
                onAccent = Color(0xFF2B0D18),
                accentSoft = Color(0xFFFDEBF1),
                accentText = Color(0xFFB84572)
            )
        }
    }
}

fun buildKovaColors(isDark: Boolean, palette: AccentPalette): KovaColors {
    val accent = resolveAccentTokens(palette, isDark)
    val isRoseLight = !isDark && palette == AccentPalette.ROSE
    return if (isDark) {
        KovaColors(
            isDark = true,
            canvas = Color(0xFF0B0C0F),
            surface = Color(0xFF15171C),
            surfaceTonal = Color(0xFF1D2027),
            surfaceElevated = Color(0xFF21252E),
            border = Color(0xFF272B35),
            borderSubtle = Color(0xFF1D2029),
            inkPrimary = Color(0xFFF4F5F7),
            inkSecondary = Color(0xFF9AA0AA),
            inkTertiary = Color(0xFF636976),
            accent = accent.accent,
            onAccent = accent.onAccent,
            accentSoft = accent.accentSoft,
            accentText = accent.accentText,
            success = Color(0xFF34D399),
            successSoft = Color(0xFF0F2B21),
            successBorder = Color(0xFF1A4A38),
            danger = Color(0xFFF87171),
            dangerSoft = Color(0xFF31161A),
            dangerBorder = Color(0xFF5A252C),
            warning = Color(0xFFFBBF24),
            warningSoft = Color(0xFF2E220D),
            warningBorder = Color(0xFF523C15),
            islandBg = Color(0xFF0F1115),
            islandSurface = Color(0xFF20242E),
            islandInk = Color(0xFFF5F6F8),
            islandInkMuted = Color(0xFF9AA0AA)
        )
    } else {
        KovaColors(
            isDark = false,
            canvas = if (isRoseLight) Color(0xFFFCF6F8) else Color(0xFFF6F5F1),
            surface = Color(0xFFFFFFFF),
            surfaceTonal = if (isRoseLight) Color(0xFFF7ECF0) else Color(0xFFEFEDE7),
            surfaceElevated = Color(0xFFFFFFFF),
            border = if (isRoseLight) Color(0xFFEEDCE3) else Color(0xFFE5E2D9),
            borderSubtle = if (isRoseLight) Color(0xFFF5E7EC) else Color(0xFFEEECE5),
            inkPrimary = Color(0xFF141518),
            inkSecondary = Color(0xFF5E626B),
            inkTertiary = Color(0xFF8C919B),
            accent = accent.accent,
            onAccent = accent.onAccent,
            accentSoft = accent.accentSoft,
            accentText = accent.accentText,
            success = Color(0xFF158A52),
            successSoft = Color(0xFFE4F6EC),
            successBorder = Color(0xFFB8E6CE),
            danger = Color(0xFFD63B48),
            dangerSoft = Color(0xFFFDEBED),
            dangerBorder = Color(0xFFF5C1C7),
            warning = Color(0xFFC27200),
            warningSoft = Color(0xFFFEF3DF),
            warningBorder = Color(0xFFF3D5A0),
            islandBg = Color(0xFF121419),
            islandSurface = Color(0xFF232731),
            islandInk = Color(0xFFF5F6F8),
            islandInkMuted = Color(0xFFA0A6B1)
        )
    }
}
