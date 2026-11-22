package io.github.firmanains.tickit.presentation.habit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.firmanains.tickit.domain.model.CheckIn
import io.github.firmanains.tickit.domain.model.HabitDetail
import io.github.firmanains.tickit.domain.usecase.ObserveHabitDetailUseCase
import io.github.firmanains.tickit.presentation.Chalk
import io.github.firmanains.tickit.presentation.common.TileGrid
import io.github.firmanains.tickit.presentation.common.TileLegend
import io.github.firmanains.tickit.presentation.habitColor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
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

private val journalDate = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id"))

@Composable
fun HabitDetailScreen(vm: HabitDetailViewModel, onBack: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val detail = state.detail
    // Surface supplies the board background and chalk content colour; without it text falls back to black.
    Surface(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
        }
        if (detail == null) {
            if (state.loaded) Text("Habit ini sudah dihapus.", Modifier.padding(20.dp))
            return@Column
        }
        val color = habitColor(detail.habit.colorIndex)
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            item {
                Text(detail.habit.name, style = MaterialTheme.typography.displayMedium, modifier = Modifier.semantics { heading() })
                Text(
                    detail.habit.reminder?.let { "Alarm setiap hari jam $it" } ?: "Tanpa alarm",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.padding(top = 24.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Stat("${detail.stats.currentStreak}", "hari beruntun", color)
                    Stat("${detail.stats.bestStreak}", "rekor terpanjang", color)
                    Stat("${detail.stats.consistencyPercent}%", "konsisten", color)
                }
                TileGrid(detail.history, detail.habit.createdOn, state.today, color, Modifier.fillMaxWidth(), cell = 13.dp, gap = 3.dp)
                TileLegend(color, Modifier.padding(top = 10.dp))
                Text(
                    "Jurnal",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 32.dp, bottom = 4.dp).semantics { heading() },
                )
                if (detail.journal.isEmpty()) {
                    Text("Belum ada catatan. Bukti pertama muncul di sini setelah kamu tick.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(detail.journal, key = { it.date.toEpochDay() }) { entry -> JournalEntry(entry, color) }
        }
    } }
}

@Composable
private fun Stat(value: String, label: String, color: Color) {
    Column {
        Text(value, style = MaterialTheme.typography.displaySmall, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Done days show the proof; excused days show the reason in red pen. A rule on the left carries the colour. */
@Composable
private fun JournalEntry(entry: CheckIn, color: Color) {
    val rule = if (entry.done) color else Chalk.RedPen
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp)
            .drawBehind { drawLine(rule, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 3.dp.toPx()) }
            .padding(start = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(entry.date.format(journalDate), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (entry.done) {
            Text(entry.note ?: "Selesai, tanpa catatan.")
            entry.photoPath?.let { path ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(File(path)).crossfade(true).build(),
                    contentDescription = "Foto bukti ${entry.date.format(journalDate)}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp)),
                )
            }
        } else {
            Text("“${entry.reason}”", color = Chalk.RedPen)
        }
    }
}
