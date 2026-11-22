package io.github.firmanains.tickit.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.firmanains.tickit.domain.dayStatus
import io.github.firmanains.tickit.domain.model.CheckIn
import io.github.firmanains.tickit.domain.model.DayStatus
import io.github.firmanains.tickit.domain.weekColumns
import io.github.firmanains.tickit.presentation.Chalk
import java.time.LocalDate

/** Fill for one tile: done in the habit's chalk, excused in red pen, missed as a smudge, nothing-yet barely there. */
fun tileColor(status: DayStatus, habit: Color): Color = when (status) {
    DayStatus.DONE -> habit
    DayStatus.EXCUSED -> Chalk.RedPen.copy(alpha = 0.75f)
    DayStatus.MISSED -> Chalk.Ink.copy(alpha = 0.16f)
    DayStatus.NONE -> Chalk.Ink.copy(alpha = 0.05f) // keeps the grid's shape, like GitHub's empty cells
}

/**
 * GitHub-style contribution grid: one column per week (Monday on top), as many weeks as fit.
 * Today is outlined while it's still open.
 */
@Composable
fun TileGrid(
    history: Map<LocalDate, CheckIn>,
    createdOn: LocalDate,
    today: LocalDate,
    color: Color,
    modifier: Modifier = Modifier,
    cell: Dp = 10.dp,
    gap: Dp = 3.dp,
) {
    BoxWithConstraints(modifier) {
        val weeks = ((maxWidth + gap) / (cell + gap)).toInt().coerceIn(1, 53)
        val columns = remember(today, weeks) { weekColumns(today, weeks) }
        val done = columns.sumOf { week -> week.count { history[it]?.done == true } }
        Canvas(
            Modifier
                .width(cell * weeks + gap * (weeks - 1))
                .height(cell * 7 + gap * 6)
                .semantics { contentDescription = "$done hari selesai dalam $weeks minggu terakhir" },
        ) {
            val c = cell.toPx()
            val g = gap.toPx()
            val radius = CornerRadius(c * 0.22f)
            columns.forEachIndexed { x, week ->
                week.forEachIndexed { y, date ->
                    val topLeft = Offset(x * (c + g), y * (c + g))
                    val status = dayStatus(history, createdOn, today, date)
                    if (date == today && status == DayStatus.NONE) {
                        drawRoundRect(color, topLeft, Size(c, c), radius, style = Stroke(1.dp.toPx()))
                    } else {
                        drawRoundRect(tileColor(status, color), topLeft, Size(c, c), radius)
                    }
                }
            }
        }
    }
}

/** Explains the three tile colours. */
@Composable
fun TileLegend(color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        listOf(DayStatus.DONE to "Selesai", DayStatus.EXCUSED to "Pakai alasan", DayStatus.MISSED to "Bolong")
            .forEach { (status, label) ->
                Spacer(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(tileColor(status, color)))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, end = 14.dp),
                )
            }
    }
}
