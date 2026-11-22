package io.github.firmanains.tickit.presentation.shame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.firmanains.tickit.domain.model.ShameEntry
import io.github.firmanains.tickit.domain.repository.HabitRepository
import io.github.firmanains.tickit.presentation.Chalk
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ShameUiState(val loaded: Boolean = false, val entries: List<ShameEntry> = emptyList())

class ShameViewModel(habits: HabitRepository) : ViewModel() {
    val uiState: StateFlow<ShameUiState> = habits.observeShame()
        .map { ShameUiState(loaded = true, entries = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShameUiState())
}

private val dateFormat = DateTimeFormatter.ofPattern("EEEE d MMM yyyy", Locale.forLanguageTag("id"))

/** Excuses written up on the board in red pen, newest first. */
@Composable
fun ShameScreen(vm: ShameViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp)) {
        item {
            Text("Wall of Shame", style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
            Text(
                when {
                    !state.loaded -> ""
                    state.entries.isEmpty() -> "Masih bersih. Semua habit yang bolong dikasih alasan bakal muncul di sini."
                    else -> "${state.entries.size} alasan tercatat. Baca, lalu jangan diulang besok."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(state.entries) { entry ->
            Column(
                Modifier.fillMaxWidth().padding(top = 16.dp)
                    .drawBehind { drawLine(Chalk.RedPen, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 3.dp.toPx()) }
                    .padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("“${entry.reason}”", style = MaterialTheme.typography.titleLarge, color = Chalk.Ink)
                Text(
                    "${entry.habitName}, ${entry.date.format(dateFormat)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
