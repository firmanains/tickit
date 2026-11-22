package io.github.firmanains.tickit.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.firmanains.tickit.domain.dayProgress
import io.github.firmanains.tickit.domain.dayStatus
import io.github.firmanains.tickit.domain.model.Habit
import io.github.firmanains.tickit.domain.model.HabitToday
import io.github.firmanains.tickit.domain.model.TodayStatus
import io.github.firmanains.tickit.presentation.Chalk
import io.github.firmanains.tickit.presentation.HandStyle
import io.github.firmanains.tickit.presentation.common.EmojiBadge
import io.github.firmanains.tickit.presentation.common.ProgressRing
import io.github.firmanains.tickit.presentation.common.ProofDialog
import io.github.firmanains.tickit.presentation.common.ReasonDialog
import io.github.firmanains.tickit.presentation.common.TileGrid
import io.github.firmanains.tickit.presentation.common.showTimePicker
import io.github.firmanains.tickit.presentation.common.tileColor
import io.github.firmanains.tickit.presentation.habitColor
import io.github.firmanains.tickit.presentation.habitColors
import io.github.firmanains.tickit.presentation.habitEmojis
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
        LazyColumn(contentPadding = PaddingValues(bottom = 112.dp)) {
            item { Header(state, compact, onToggleLayout = { compact = !compact }) }
            item { PermissionBanners(resumes) }
            if (state.loaded && state.habits.isEmpty()) {
                item { EmptyState(onAdd = { adding = true }) }
            } else if (state.habits.isNotEmpty()) {
                item { WeekStrip(state.habits, state.today) }
            }
            if (compact && state.habits.isNotEmpty()) {
                item { CompactHeader(state.today) }
                items(state.habits, key = { it.habit.id }) { item ->
                    CompactRow(item, state.today, onOpen = { onOpenHabit(item.habit.id) }, onCheck = { onCheck(item) })
                }
            } else {
                items(state.habits, key = { it.habit.id }) { item ->
                    HabitCard(
                        item = item,
                        today = state.today,
                        onOpen = { onOpenHabit(item.habit.id) },
                        onCheck = { onCheck(item) },
                        onExcuse = { excusing = item.habit },
                        onDelete = { deleting = item.habit },
                    )
                }
            }
            if (state.habits.isNotEmpty()) {
                item { EndOfDayCard(state.endOfDay, onChange = { showTimePicker(context, state.endOfDay, vm::changeEndOfDay) }) }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { adding = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Habit baru") },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        )
    }

    if (adding) {
        AddHabitDialog(
            onDismiss = { adding = false },
            onSave = { name, emoji, reminder, color -> vm.add(name, emoji, reminder, color); adding = false },
        )
    }
    proofFor?.let { habit ->
        ProofDialog(
            habit = habit,
            newPhotoPath = vm::newPhotoPath,
            onDiscardPhoto = vm::discardPhoto,
            onDismiss = { proofFor = null },
            onSubmit = { vm.complete(habit.id, it); proofFor = null },
        )
    }
    excusing?.let { habit ->
        ReasonDialog(habit, onDismiss = { excusing = null }) { vm.excuse(habit.id, it); excusing = null }
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
            text = { Text("Alarmnya ikut mati. Alasan yang sudah ditempel tetap ada di Wall of Shame.") },
            confirmButton = { TextButton(onClick = { vm.delete(habit.id); deleting = null }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Batal") } },
        )
    }
}

private fun summary(habits: List<HabitToday>): String {
    val done = habits.count { it.status == TodayStatus.DONE }
    val excused = habits.count { it.status == TodayStatus.EXCUSED }
    val open = habits.size - done - excused
    return when {
        done == habits.size -> "Semua selesai. Mantap! 🎉"
        done == 0 && excused == 0 -> "Belum ada yang dicentang. Mulai dari yang paling gampang."
        open == 0 -> "Semua sudah dibereskan, $excused pakai alasan."
        open == 1 -> "Tinggal satu lagi. Gas! 💪"
        else -> "$done selesai, $open lagi menunggu."
    }
}

@Composable
private fun Header(state: HomeUiState, compact: Boolean, onToggleLayout: () -> Unit) {
    val done = state.habits.count { it.status == TodayStatus.DONE }
    Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(state.today.format(headerDate), style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
            if (state.habits.isNotEmpty()) {
                Text(summary(state.habits), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (state.habits.isNotEmpty()) {
            ProgressRing(
                progress = done.toFloat() / state.habits.size,
                color = MaterialTheme.colorScheme.primary,
                stroke = 6.dp,
                modifier = Modifier.size(64.dp).semantics { contentDescription = "$done dari ${state.habits.size} selesai hari ini" },
            ) {
                Text("$done/${state.habits.size}", style = MaterialTheme.typography.titleLarge)
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

/** The last seven days as rings: how much of each day got done. */
@Composable
private fun WeekStrip(habits: List<HabitToday>, today: LocalDate) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        (6L downTo 0L).map(today::minusDays).forEach { day ->
            val progress = dayProgress(habits, day) ?: 0f
            val isToday = day == today
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(44.dp)) {
                Text(
                    day.format(shortDay),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ProgressRing(
                    progress = progress,
                    color = if (isToday) MaterialTheme.colorScheme.primary else Chalk.Ink.copy(alpha = 0.85f),
                    stroke = 3.dp,
                    modifier = Modifier.padding(top = 4.dp).size(40.dp)
                        .semantics { contentDescription = "${day.format(shortDay)} ${day.dayOfMonth}: ${(progress * 100).toInt()} persen selesai" },
                ) {
                    Text("${day.dayOfMonth}", style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp))
                }
            }
        }
    }
}

@Composable
private fun HabitCard(
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
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(lerp(Chalk.Slate, color, 0.07f))
            .clickable(onClickLabel = "Buka riwayat ${item.habit.name}", onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(item.habit.emoji, color)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(item.habit.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Chip("🔥 ${item.streak} hari", color)
                    item.habit.reminder?.let { Chip("⏰ $it", color) }
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
            modifier = Modifier.padding(top = 14.dp, end = 12.dp).fillMaxWidth(),
        )
        if (item.status == TodayStatus.EXCUSED && item.reason != null) {
            Text("😶 “${item.reason}”", style = HandStyle, color = Chalk.RedPen, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/**
 * 48dp target: outline = still open, filled = done, red = excused.
 * When a tick lands it pops and throws a little burst of chalk in the habit's colour.
 */
@Composable
private fun CheckButton(item: HabitToday, color: Color, onClick: () -> Unit) {
    val name = item.habit.name
    val done = item.status == TodayStatus.DONE
    val haptics = LocalHapticFeedback.current
    val burst = remember { Animatable(1f) }
    var wasDone by remember { mutableStateOf(done) }
    LaunchedEffect(done) {
        if (done && !wasDone) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            burst.snapTo(0f)
            burst.animateTo(1f, tween(700))
        }
        wasDone = done
    }
    val pop by animateFloatAsState(
        if (done) 1f else 0.9f,
        spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "pop",
    )
    val (label, fill, ring) = when (item.status) {
        TodayStatus.PENDING -> Triple("Tandai $name selesai", Color.Transparent, color)
        TodayStatus.DONE -> Triple("Batalkan tick $name", color, color)
        TodayStatus.EXCUSED -> Triple("Ganti alasan $name dengan bukti", Chalk.RedPen, Chalk.RedPen)
    }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .semantics { contentDescription = label }
            .drawBehind {
                val t = burst.value
                if (t < 1f) {
                    repeat(10) { i ->
                        val angle = i * (2 * PI / 10) + 0.3
                        val distance = (20 + 22 * t).dp.toPx()
                        drawCircle(
                            color = if (i % 3 == 0) Chalk.Alarm else color,
                            radius = (3.5f * (1 - t) + 0.5f).dp.toPx(),
                            center = center + Offset((cos(angle) * distance).toFloat(), (sin(angle) * distance).toFloat()),
                            alpha = 1 - t,
                        )
                    }
                }
            },
    ) {
        Box(
            Modifier.size(36.dp).graphicsLayer { scaleX = pop; scaleY = pop }
                .clip(CircleShape).background(fill).border(2.5.dp, ring, CircleShape),
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
private fun CompactHeader(today: LocalDate) {
    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
        Spacer(Modifier.weight(1f))
        (6L downTo 0L).map(today::minusDays).forEach { day ->
            Text(
                day.format(shortDay),
                style = MaterialTheme.typography.labelSmall,
                color = if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(36.dp).padding(start = 8.dp),
            )
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
        EmojiBadge(item.habit.emoji, color, size = 32.dp)
        Text(item.habit.name, Modifier.weight(1f).padding(start = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        (6L downTo 0L).map(today::minusDays).forEach { day ->
            val status = dayStatus(item.history, item.habit.createdOn, today, day)
            val tile = Modifier.padding(4.dp).size(28.dp).clip(RoundedCornerShape(8.dp))
            if (day == today) {
                Box(
                    tile.background(tileColor(status, color))
                        .border(1.5.dp, if (item.status == TodayStatus.EXCUSED) Chalk.RedPen else color, RoundedCornerShape(8.dp))
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
private fun EndOfDayCard(time: LocalTime, onChange: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Chalk.Slate)
            .clickable(onClickLabel = "Ubah jam review", onClick = onChange)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiBadge("🌙", Chalk.Alarm)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
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
private fun EmptyState(onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🌱", fontSize = 64.sp)
        Text("Papannya masih kosong", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
        Text(
            "Mulai dari satu habit kecil, misalnya minum air.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
        )
        Button(onClick = onAdd) { Text("Tambah habit pertama") }
    }
}

@Composable
private fun AddHabitDialog(onDismiss: () -> Unit, onSave: (String, String, LocalTime?, Int) -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf(habitEmojis.first()) }
    var reminder by rememberSaveable { mutableStateOf<LocalTime?>(null) }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(emoji, habitColors[colorIndex], size = 40.dp)
                Text(name.ifBlank { "Habit baru" }, Modifier.padding(start = 12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama habit") }, singleLine = true)
                Text("Emoji", style = MaterialTheme.typography.labelLarge)
                habitEmojis.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { option ->
                            val chosen = option == emoji
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                    .background(if (chosen) habitColors[colorIndex].copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable { emoji = option }
                                    .semantics { contentDescription = "Emoji $option"; selected = chosen },
                                contentAlignment = Alignment.Center,
                            ) { Text(option, fontSize = 24.sp) }
                        }
                    }
                }
                Text("Warna", style = MaterialTheme.typography.labelLarge)
                habitColors.indices.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showTimePicker(context, reminder ?: LocalTime.of(7, 0)) { reminder = it } }) {
                        Text(reminder?.let { "⏰ Alarm jam $it" } ?: "⏰ Pasang alarm")
                    }
                    if (reminder != null) TextButton(onClick = { reminder = null }) { Text("Tanpa alarm") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, emoji, reminder, colorIndex) }, enabled = name.isNotBlank()) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
