package com.kova.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun KovaLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = Color(0xFF111318),
    plateColor: Color = Color(0xFFFFFFFF),
    outerPlateColor: Color = Color(0xFFC9D0DC),
    accentCollarColor: Color = Color(0xFFC6F635)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val corner = w * 0.27f

        // Obsidian studio squircle background
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF232833), containerColor, Color(0xFF0C0E12)),
                center = Offset(w * 0.5f, h * 0.42f),
                radius = w * 0.78f
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner)
        )

        // Subtle ambient Volt glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentCollarColor.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.42f
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Sculptural tilted dumbbell emblem (-32 deg)
        rotate(degrees = -32f, pivot = Offset(w * 0.5f, h * 0.5f)) {
            val barH = h * 0.088f
            val barW = w * 0.34f
            drawRoundRect(
                color = Color(0xFFD8DDE7),
                topLeft = Offset((w - barW) / 2f, (h - barH) / 2f),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barH / 2f, barH / 2f)
            )

            // Dual accent locking collars
            val collarW = w * 0.044f
            val collarH = h * 0.22f
            val collarOffset = w * 0.145f
            listOf(-1f, 1f).forEach { dir ->
                val cx = w * 0.5f + dir * collarOffset
                drawRoundRect(
                    color = accentCollarColor,
                    topLeft = Offset(cx - collarW / 2f, (h - collarH) / 2f),
                    size = Size(collarW, collarH),
                    cornerRadius = CornerRadius(collarW / 2f, collarW / 2f)
                )
            }

            // Inner tall machined plates
            val innerW = w * 0.098f
            val innerH = h * 0.45f
            val innerOffset = w * 0.228f
            listOf(-1f, 1f).forEach { dir ->
                val cx = w * 0.5f + dir * innerOffset
                drawRoundRect(
                    color = plateColor,
                    topLeft = Offset(cx - innerW / 2f, (h - innerH) / 2f),
                    size = Size(innerW, innerH),
                    cornerRadius = CornerRadius(innerW / 2f, innerW / 2f)
                )
            }

            // Outer compact plates
            val outerW = w * 0.078f
            val outerH = h * 0.31f
            val outerOffset = w * 0.332f
            listOf(-1f, 1f).forEach { dir ->
                val cx = w * 0.5f + dir * outerOffset
                drawRoundRect(
                    color = outerPlateColor,
                    topLeft = Offset(cx - outerW / 2f, (h - outerH) / 2f),
                    size = Size(outerW, outerH),
                    cornerRadius = CornerRadius(outerW / 2f, outerW / 2f)
                )
            }
        }
    }
}
