package io.github.firmanains.tickit.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.firmanains.tickit.domain.dayStatus
import io.github.firmanains.tickit.domain.model.Habit
import io.github.firmanains.tickit.domain.model.HabitToday
import io.github.firmanains.tickit.domain.model.TodayStatus
import io.github.firmanains.tickit.presentation.Chalk
import io.github.firmanains.tickit.presentation.common.ProofDialog
import io.github.firmanains.tickit.presentation.common.ReasonDialog
import io.github.firmanains.tickit.presentation.common.TileGrid
import io.github.firmanains.tickit.presentation.common.showTimePicker
import io.github.firmanains.tickit.presentation.common.tileColor
import io.github.firmanains.tickit.presentation.habitColor
import io.github.firmanains.tickit.presentation.habitColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val indonesian = Locale.forLanguageTag("id")
private val headerDate = DateTimeFormatter.ofPattern("EEEE, d MMM", indonesian)
private val shortDay = DateTimeFormatter.ofPattern("EEE", indonesian)

@Composable
fun HomeScreen(vm: HomeViewModel, onOpenHabit: (Long) -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var resumes by remember { mutableIntStateOf(0) }
    var compact by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var proofFor by remember { mutableStateOf<Habit?>(null) }
    var excusing by remember { mutableStateOf<Habit?>(null) }
    var undoing by remember { mutableStateOf<Habit?>(null) }
    var deleting by remember { mutableStateOf<Habit?>(null) }

    LifecycleResumeEffect(Unit) {
        vm.refreshDay()
        resumes++ // re-check permissions after returning from Settings
        onPauseOrDispose {}
    }

    val onCheck: (HabitToday) -> Unit = { item -> if (item.status == TodayStatus.DONE) undoing = item.habit else proofFor = item.habit }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Header(state, compact, onToggleLayout = { compact = !compact })
            }
            item { PermissionBanners(resumes) }
            if (state.loaded && state.habits.isEmpty()) {
                item {
                    Text(
                        "Belum ada habit. Tambah yang pertama lewat tombol +.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    )
                }
            }
            if (compact && state.habits.isNotEmpty()) {
                item { WeekHeader(state.today) }
                items(state.habits, key = { it.habit.id }) { item ->
                    CompactRow(item, state.today, onOpen = { onOpenHabit(item.habit.id) }, onCheck = { onCheck(item) })
                }
            } else {
                items(state.habits, key = { it.habit.id }) { item ->
                    HabitBlock(
                        item = item,
                        today = state.today,
                        onOpen = { onOpenHabit(item.habit.id) },
                        onCheck = { onCheck(item) },
                        onExcuse = { excusing = item.habit },
                        onDelete = { deleting = item.habit },
                    )
                }
            }
            item {
                EndOfDayRow(state.endOfDay, onChange = { showTimePicker(context, state.endOfDay, vm::changeEndOfDay) })
            }
        }
        FloatingActionButton(
            onClick = { adding = true },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) { Icon(Icons.Default.Add, contentDescription = "Tambah habit") }
    }

    if (adding) {
        AddHabitDialog(
            onDismiss = { adding = false },
            onSave = { name, reminder, color -> vm.add(name, reminder, color); adding = false },
        )
    }
    proofFor?.let { habit ->
        ProofDialog(
            habitName = habit.name,
            newPhotoPath = vm::newPhotoPath,
            onDiscardPhoto = vm::discardPhoto,
            onDismiss = { proofFor = null },
            onSubmit = { vm.complete(habit.id, it); proofFor = null },
        )
    }
    excusing?.let { habit ->
        ReasonDialog(habit.name, onDismiss = { excusing = null }) { vm.excuse(habit.id, it); excusing = null }
    }
    undoing?.let { habit ->
        AlertDialog(
            onDismissRequest = { undoing = null },
            title = { Text("Batalkan tick ${habit.name}?") },
            text = { Text("Catatan dan foto buktinya ikut terhapus.") },
            confirmButton = { TextButton(onClick = { vm.undo(habit.id); undoing = null }) { Text("Batalkan tick") } },
            dismissButton = { TextButton(onClick = { undoing = null }) { Text("Biarkan") } },
        )
    }
    deleting?.let { habit ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Hapus ${habit.name}?") },
            text = { Text("Alarmnya ikut mati. Alasan yang sudah dipajang tetap ada di Wall of Shame.") },
            confirmButton = { TextButton(onClick = { vm.delete(habit.id); deleting = null }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Batal") } },
        )
    }
}

@Composable
private fun Header(state: HomeUiState, compact: Boolean, onToggleLayout: () -> Unit) {
    Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 24.dp, bottom = 8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                state.today.format(headerDate),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.semantics { heading() },
            )
            if (state.habits.isNotEmpty()) {
                Text(
                    if (state.settled == state.habits.size) "Semua sudah dibereskan hari ini."
                    else "${state.settled} dari ${state.habits.size} sudah dibereskan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onToggleLayout) {
            Icon(
                if (compact) Icons.Default.DateRange else Icons.AutoMirrored.Filled.List,
                contentDescription = if (compact) "Tampilkan tile riwayat" else "Tampilkan ringkas 7 hari",
            )
        }
    }
}

@Composable
private fun HabitBlock(
    item: HabitToday,
    today: LocalDate,
    onOpen: () -> Unit,
    onCheck: () -> Unit,
    onExcuse: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = habitColor(item.habit.colorIndex)
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Buka riwayat ${item.habit.name}", onClick = onOpen)
            .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.habit.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🔥 ${item.streak} hari", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    item.habit.reminder?.let { Text("⏰ $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            CheckButton(item, color, onCheck)
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Opsi ${item.habit.name}") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (item.status == TodayStatus.PENDING) {
                        DropdownMenuItem(text = { Text("Nggak bisa, tulis alasan") }, onClick = { menu = false; onExcuse() })
                    }
                    DropdownMenuItem(text = { Text("Hapus habit") }, onClick = { menu = false; onDelete() })
                }
            }
        }
        TileGrid(
            history = item.history,
            createdOn = item.habit.createdOn,
            today = today,
            color = color,
            modifier = Modifier.padding(top = 12.dp, end = 12.dp).fillMaxWidth(),
        )
        if (item.status == TodayStatus.EXCUSED && item.reason != null) {
            Text("“${item.reason}”", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** 48dp target; outline = still open, filled = done with proof, red = excused. */
@Composable
private fun CheckButton(item: HabitToday, color: Color, onClick: () -> Unit) {
    val name = item.habit.name
    val (label, fill, ring) = when (item.status) {
        TodayStatus.PENDING -> Triple("Tandai $name selesai", Color.Transparent, color)
        TodayStatus.DONE -> Triple("Batalkan tick $name", color, color)
        TodayStatus.EXCUSED -> Triple("Ganti alasan $name dengan bukti", Chalk.RedPen, Chalk.RedPen)
    }
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = label }) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(fill).border(2.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (item.status) {
                TodayStatus.DONE -> Icon(Icons.Default.Check, contentDescription = null, tint = Chalk.Board)
                TodayStatus.EXCUSED -> Icon(Icons.Default.Close, contentDescription = null, tint = Chalk.Board)
                TodayStatus.PENDING -> Unit
            }
        }
    }
}

@Composable
private fun WeekHeader(today: LocalDate) {
    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
        Spacer(Modifier.weight(1f))
        (6L downTo 0L).map(today::minusDays).forEach { day ->
            Column(Modifier.width(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(day.format(shortDay), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${day.dayOfMonth}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** The "efficient list": one row per habit, the last seven days as tiles; today's tile is the check button. */
@Composable
private fun CompactRow(item: HabitToday, today: LocalDate, onOpen: () -> Unit, onCheck: () -> Unit) {
    val color = habitColor(item.habit.colorIndex)
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = "Buka riwayat ${item.habit.name}", onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(item.habit.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        (6L downTo 0L).map(today::minusDays).forEach { day ->
            val status = dayStatus(item.history, item.habit.createdOn, today, day)
            val tile = Modifier.padding(4.dp).size(28.dp).clip(RoundedCornerShape(6.dp))
            if (day == today) {
                Box(
                    tile.background(tileColor(status, color))
                        .border(1.5.dp, if (item.status == TodayStatus.EXCUSED) Chalk.RedPen else color, RoundedCornerShape(6.dp))
                        .clickable(onClick = onCheck)
                        .semantics {
                            contentDescription = if (item.status == TodayStatus.DONE) "Batalkan tick ${item.habit.name}" else "Tandai ${item.habit.name} selesai"
                        },
                )
            } else {
                Box(tile.background(tileColor(status, color)))
            }
        }
    }
}

@Composable
private fun EndOfDayRow(time: LocalTime, onChange: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = "Ubah jam review", onClick = onChange).padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Review akhir hari")
            Text(
                "Alarm bunyi kalau masih ada yang belum dibereskan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("$time", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AddHabitDialog(onDismiss: () -> Unit, onSave: (String, LocalTime?, Int) -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var reminder by rememberSaveable { mutableStateOf<LocalTime?>(null) }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Habit baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama habit") }, singleLine = true)
                Text("Warna", style = MaterialTheme.typography.labelLarge)
                habitColors.indices.chunked(4).forEach { row ->
                    Row {
                        row.forEach { index ->
                            val chosen = index == colorIndex
                            Box(
                                Modifier.size(48.dp).clickable { colorIndex = index }
                                    .semantics { contentDescription = "Warna ${index + 1}"; selected = chosen },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier.size(30.dp).clip(CircleShape).background(habitColors[index])
                                        .border(if (chosen) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                                )
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showTimePicker(context, reminder ?: LocalTime.of(7, 0)) { reminder = it } }) {
                        Text(reminder?.let { "Alarm jam $it" } ?: "Pasang alarm")
                    }
                    if (reminder != null) TextButton(onClick = { reminder = null }) { Text("Tanpa alarm") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, reminder, colorIndex) }, enabled = name.isNotBlank()) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
