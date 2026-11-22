package io.github.firmanains.tickit.presentation.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.firmanains.tickit.domain.model.CheckIn
import io.github.firmanains.tickit.domain.model.HabitDetail
import io.github.firmanains.tickit.domain.usecase.ObserveHabitDetailUseCase
import io.github.firmanains.tickit.presentation.Chalk
import io.github.firmanains.tickit.presentation.HandStyle
import io.github.firmanains.tickit.presentation.common.EmojiBadge
import io.github.firmanains.tickit.presentation.common.Polaroid
import io.github.firmanains.tickit.presentation.common.TileGrid
import io.github.firmanains.tickit.presentation.common.TileLegend
import io.github.firmanains.tickit.presentation.common.tiltFor
import io.github.firmanains.tickit.presentation.habitColor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class HabitDetailUiState(val loaded: Boolean = false, val today: LocalDate = LocalDate.now(), val detail: HabitDetail? = null)

class HabitDetailViewModel(habitId: Long, observeDetail: ObserveHabitDetailUseCase) : ViewModel() {
    private val today = LocalDate.now()
    val uiState: StateFlow<HabitDetailUiState> = observeDetail(habitId, today)
        .map { HabitDetailUiState(loaded = true, today = today, detail = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitDetailUiState())
}

private val indonesian = Locale.forLanguageTag("id")
private val journalDate = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", indonesian)
private val polaroidDate = DateTimeFormatter.ofPattern("d MMM", indonesian)

@Composable
fun HabitDetailScreen(vm: HabitDetailViewModel, onBack: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    // Surface supplies the board background and chalk content colour; without it text falls back to black.
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            val detail = state.detail
            if (detail != null) DetailContent(detail, state.today)
            else if (state.loaded) Text("Habit ini sudah dihapus.", Modifier.padding(20.dp))
        }
    }
}

@Composable
private fun DetailContent(detail: HabitDetail, today: LocalDate) {
    val color = habitColor(detail.habit.colorIndex)
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(detail.habit.emoji, color, size = 64.dp)
                Column(Modifier.padding(start = 16.dp)) {
                    Text(detail.habit.name, style = MaterialTheme.typography.displayMedium, modifier = Modifier.semantics { heading() })
                    Text(
                        detail.habit.reminder?.let { "⏰ Alarm setiap hari jam $it" } ?: "Tanpa alarm",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("${detail.stats.currentStreak}", "🔥 beruntun", color, Modifier.weight(1f))
                Stat("${detail.stats.bestStreak}", "🏆 rekor", color, Modifier.weight(1f))
                Stat("${detail.stats.consistencyPercent}%", "🎯 konsisten", color, Modifier.weight(1f))
            }
            Column(
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(lerp(Chalk.Slate, color, 0.06f)).padding(16.dp),
            ) {
                TileGrid(detail.history, detail.habit.createdOn, today, color, Modifier.fillMaxWidth(), cell = 12.dp, gap = 3.dp)
                TileLegend(color, Modifier.padding(top = 12.dp))
            }
            Text(
                "Jurnal",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 32.dp, bottom = 8.dp).semantics { heading() },
            )
            if (detail.journal.isEmpty()) {
                Text("Belum ada catatan. Bukti pertamamu bakal muncul di sini. ✍️", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(detail.journal, key = { it.date.toEpochDay() }) { entry -> JournalEntry(entry, color) }
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(lerp(Chalk.Slate, color, 0.1f)).padding(14.dp)) {
        Text(value, style = MaterialTheme.typography.displaySmall, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A timeline entry: dot in the habit's colour (or red pen), date, then the user's own words in handwriting. */
@Composable
private fun JournalEntry(entry: CheckIn, color: Color) {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Box(Modifier.padding(top = 5.dp).size(10.dp).clip(CircleShape).background(if (entry.done) color else Chalk.RedPen))
        Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.date.format(journalDate), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (entry.done) {
                Text(entry.note ?: "Selesai, tanpa catatan.", style = HandStyle)
                entry.photoPath?.let { path ->
                    Polaroid(
                        path = path,
                        caption = entry.date.format(polaroidDate),
                        contentDescription = "Foto bukti ${entry.date.format(journalDate)}",
                        tilt = tiltFor(entry.date),
                        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp).fillMaxWidth(0.78f),
                    )
                }
            } else {
                Text("😶 “${entry.reason}”", style = HandStyle, color = Chalk.RedPen)
            }
        }
    }
}
