package io.github.firmanains.tickit.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.firmanains.tickit.R

/** Blackboard palette: chalk on slate, red pen for excuses, amber for the alarm. */
object Chalk {
    val Board = Color(0xFF17201C)
    val Slate = Color(0xFF212C27)
    val SlateHigh = Color(0xFF2C3933)
    val Ink = Color(0xFFEDEBE3)
    val Dust = Color(0xFF8C978F)
    val Alarm = Color(0xFFFFB547)
    val RedPen = Color(0xFFFF6B5E)
}

/** Chalk pastels a habit can be drawn in; stored as an index so the domain stays UI-free. */
val habitColors = listOf(
    Color(0xFFF7D154), Color(0xFFF28B82), Color(0xFF7DD3A8), Color(0xFF7CC4F2),
    Color(0xFFC3A6F2), Color(0xFFF5A96B), Color(0xFF8EE0D6), Color(0xFFE8E2C8),
)

fun habitColor(index: Int) = habitColors[index.mod(habitColors.size)]

@OptIn(ExperimentalTextApi::class)
private fun display(weight: Int) =
    Font(R.font.big_shoulders_display, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

/** Condensed scoreboard face for numbers, times and titles; body text stays system Roboto. */
val Scoreboard = FontFamily(display(500), display(700), display(800))

private val typography = Typography().run {
    copy(
        displayLarge = TextStyle(fontFamily = Scoreboard, fontWeight = FontWeight(800), fontSize = 96.sp, lineHeight = 96.sp),
        displayMedium = TextStyle(fontFamily = Scoreboard, fontWeight = FontWeight(800), fontSize = 48.sp, lineHeight = 50.sp),
        displaySmall = TextStyle(fontFamily = Scoreboard, fontWeight = FontWeight(700), fontSize = 36.sp, lineHeight = 40.sp),
        headlineSmall = TextStyle(fontFamily = Scoreboard, fontWeight = FontWeight(700), fontSize = 24.sp, lineHeight = 28.sp),
        titleLarge = TextStyle(fontFamily = Scoreboard, fontWeight = FontWeight(700), fontSize = 22.sp, lineHeight = 26.sp),
    )
}

private val colors = darkColorScheme(
    primary = Chalk.Alarm,
    onPrimary = Chalk.Board,
    secondary = Chalk.Ink,
    onSecondary = Chalk.Board,
    secondaryContainer = Chalk.SlateHigh,
    onSecondaryContainer = Chalk.Ink,
    background = Chalk.Board,
    onBackground = Chalk.Ink,
    surface = Chalk.Board,
    onSurface = Chalk.Ink,
    surfaceVariant = Chalk.Slate,
    onSurfaceVariant = Chalk.Dust,
    surfaceContainerLowest = Chalk.Board,
    surfaceContainerLow = Chalk.Slate,
    surfaceContainer = Chalk.Slate,
    surfaceContainerHigh = Chalk.SlateHigh,
    surfaceContainerHighest = Chalk.SlateHigh,
    error = Chalk.RedPen,
    onError = Chalk.Board,
    errorContainer = Chalk.Slate,
    onErrorContainer = Chalk.RedPen,
    outline = Chalk.Dust,
    outlineVariant = Color(0xFF3A4842),
)

@Composable
fun TickitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
