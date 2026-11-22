package io.github.firmanains.tickit.presentation.shame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.firmanains.tickit.domain.model.ShameEntry
import io.github.firmanains.tickit.domain.repository.HabitRepository
import io.github.firmanains.tickit.presentation.HandStyle
import io.github.firmanains.tickit.presentation.Paper
import io.github.firmanains.tickit.presentation.common.tiltFor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TopOffender(val label: String, val count: Int)

data class ShameUiState(
    val loaded: Boolean = false,
    val entries: List<ShameEntry> = emptyList(),
    val topOffender: TopOffender? = null,
)

class ShameViewModel(habits: HabitRepository) : ViewModel() {
    val uiState: StateFlow<ShameUiState> = habits.observeShame()
        .map { entries ->
            val top = entries.groupingBy { "${it.habitEmoji} ${it.habitName}" }.eachCount().maxByOrNull { it.value }
            ShameUiState(loaded = true, entries = entries, topOffender = top?.let { TopOffender(it.key, it.value) })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShameUiState())
}

private val dateFormat = DateTimeFormatter.ofPattern("EEEE d MMM yyyy", Locale.forLanguageTag("id"))

/** Excuses pinned to the board as sticky notes, newest first. */
@Composable
fun ShameScreen(vm: ShameViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalItemSpacing = 18.dp,
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Column(Modifier.padding(start = 4.dp, bottom = 8.dp)) {
                Text("Wall of Shame", style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
                Text(
                    when {
                        !state.loaded -> ""
                        state.entries.isEmpty() -> "Masih bersih. Alasan yang kamu tulis bakal ditempel di sini."
                        else -> "${state.entries.size} alasan ditempel. Baca, lalu jangan diulang besok."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.topOffender?.let {
                    Text(
                        "Paling sering: ${it.label} (${it.count} alasan)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
        if (state.loaded && state.entries.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Text("✨", fontSize = 56.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 40.dp))
            }
        }
        items(state.entries, key = { "${it.habitName}-${it.date}" }) { entry -> StickyNote(entry) }
    }
}

/** Yellow note, red pen, a strip of tape on top, and a small hand-placed tilt. */
@Composable
private fun StickyNote(entry: ShameEntry) {
    val tilt = tiltFor("${entry.habitName}${entry.date}")
    Column(
        Modifier
            .padding(top = 8.dp)
            .graphicsLayer { rotationZ = tilt }
            .drawWithContent {
                drawContent()
                val tape = Size(52.dp.toPx(), 16.dp.toPx())
                drawRect(Color.White.copy(alpha = 0.35f), Offset((size.width - tape.width) / 2, -tape.height / 2), tape)
            }
            .shadow(6.dp, RoundedCornerShape(3.dp))
            .background(Paper.Sticky, RoundedCornerShape(3.dp))
            .padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("“${entry.reason}”", style = HandStyle.copy(fontSize = 22.sp, lineHeight = 25.sp), color = Paper.RedInk)
        Column {
            Text("${entry.habitEmoji} ${entry.habitName}", style = MaterialTheme.typography.labelLarge, color = Paper.Ink)
            Text(entry.date.format(dateFormat), style = MaterialTheme.typography.labelSmall, color = Paper.Ink.copy(alpha = 0.7f))
        }
    }
}
