package com.mangareader.local

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Fixed dark theme, same colors as the web page. */
private val Scheme = darkColorScheme(
    primary = Color(0xFF4DA3FF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1F3A5F),
    onPrimaryContainer = Color(0xFFD6E8FF),
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color(0xFFEEEEEE),
    background = Color(0xFF111111),
    onBackground = Color(0xFFEEEEEE),
    surface = Color(0xFF161616),
    onSurface = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFFA0A0A0),
    surfaceContainerLowest = Color(0xFF111111),
    surfaceContainerLow = Color(0xFF1D1D1D),
    surfaceContainer = Color(0xFF202020),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF2E2E2E),
    outline = Color(0xFF555555),
    outlineVariant = Color(0xFF333333),
)

object AppColors {
    val furigana = Color(0xFFFFD166)
    val grammar = Color(0xFF7BD88F)
    val particle = Color(0xFFC792EA)
}

@Composable
fun MangaTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, content = content)

/** Open book outline icon for "My words" (not in the core icon set). */
val BookIcon: ImageVector = ImageVector.Builder("book", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.White), strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(12f, 6f); curveTo(10f, 4.5f, 6.5f, 4f, 3f, 4.5f); lineTo(3f, 19f)
        curveTo(6.5f, 18.5f, 10f, 19f, 12f, 20.5f); close()
        moveTo(12f, 6f); curveTo(14f, 4.5f, 17.5f, 4f, 21f, 4.5f); lineTo(21f, 19f)
        curveTo(17.5f, 18.5f, 14f, 19f, 12f, 20.5f)
    }
}.build()

/** Flashlight outline icon (not in the core icon set). */
val FlashlightIcon: ImageVector = ImageVector.Builder("flashlight", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.White), strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(7f, 2f); lineTo(17f, 2f); lineTo(17f, 6f); lineTo(14f, 10f); lineTo(14f, 22f)
        lineTo(10f, 22f); lineTo(10f, 10f); lineTo(7f, 6f); close()
        moveTo(12f, 13f); lineTo(12f, 15f)
    }
}.build()
