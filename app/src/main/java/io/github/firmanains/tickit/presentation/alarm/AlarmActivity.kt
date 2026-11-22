package io.github.firmanains.tickit.presentation.alarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.firmanains.tickit.alarm.AlarmIntents
import io.github.firmanains.tickit.container
import io.github.firmanains.tickit.domain.AlarmPolicy
import io.github.firmanains.tickit.domain.model.Habit
import io.github.firmanains.tickit.presentation.Chalk
import io.github.firmanains.tickit.presentation.TickitTheme
import io.github.firmanains.tickit.presentation.common.EmojiBadge
import io.github.firmanains.tickit.presentation.common.ProofDialog
import io.github.firmanains.tickit.presentation.common.ReasonDialog
import io.github.firmanains.tickit.presentation.habitColor
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private data class AlarmRequest(val habitId: Long?, val snoozesUsed: Int)

class AlarmActivity : ComponentActivity() {
    private var request by mutableStateOf(AlarmRequest(null, 0))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        request = intent.toRequest()
        val c = container
        setContent {
            TickitTheme {
                val r = request
                key(r) {
                    val vm = viewModel(key = r.toString()) { c.alarmViewModel(r.habitId, r.snoozesUsed) }
                    AlarmScreen(vm, onStop = {
                        c.notifier.stop(r.habitId) // silences the looping sound
                        finish()
                    })
                }
            }
        }
    }

    // singleInstance: a second alarm while this one is showing arrives here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // so a rotation rebuilds this alarm, not the first one
        request = intent.toRequest()
    }

    private fun Intent.toRequest() = AlarmRequest(AlarmIntents.habitId(this), AlarmIntents.snoozesUsed(this))
}

@Composable
private fun AlarmScreen(vm: AlarmViewModel, onStop: () -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var proofFor by remember { mutableStateOf<Habit?>(null) }
    var excusing by remember { mutableStateOf<Habit?>(null) }
    val now = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }
    val pending = state.pending ?: return

    LaunchedEffect(pending.isEmpty()) { if (pending.isEmpty()) onStop() }

    // The one motion that runs on its own: the alarm is supposed to grab attention.
    val wiggle by rememberInfiniteTransition(label = "wiggle").animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(180), RepeatMode.Reverse),
        label = "wiggle",
    )
    val emoji = if (state.isEndOfDay) "🌙" else pending.firstOrNull()?.habit?.emoji ?: "⏰"

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(now, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Text(emoji, fontSize = 64.sp, modifier = Modifier.graphicsLayer { rotationZ = wiggle })
            }
            Text(
                if (state.isEndOfDay) "Review akhir hari" else pending.firstOrNull()?.habit?.name.orEmpty(),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                if (state.isEndOfDay) "Masih ada yang belum dibereskan. Kasih bukti, atau tulis kenapa nggak jalan."
                else "Kerjakan sekarang dan kasih bukti, tunda, atau tulis kenapa nggak jalan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            val single = pending.firstOrNull()?.habit
            if (!state.isEndOfDay && single != null) {
                // One habit: its name is already the headline, so just two big answers.
                Column(Modifier.weight(1f).padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { proofFor = single }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                        Text("Selesai, isi bukti", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedButton(onClick = { excusing = single }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                        Text("Nggak bisa, tulis alasan", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else LazyColumn(Modifier.weight(1f).padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(pending, key = { it.habit.id }) { item ->
                    val color = habitColor(item.habit.colorIndex)
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(lerp(Chalk.Slate, color, 0.08f)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EmojiBadge(item.habit.emoji, color)
                            Text(item.habit.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 12.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { proofFor = item.habit }) { Text("Selesai, isi bukti") }
                            OutlinedButton(onClick = { excusing = item.habit }) { Text("Nggak bisa") }
                        }
                    }
                }
            }
            if (state.snoozesLeft > 0) {
                OutlinedButton(onClick = { if (vm.snooze()) onStop() }, modifier = Modifier.fillMaxWidth()) {
                    Text("😴 Tunda ${AlarmPolicy.SNOOZE_MINUTES} menit (sisa ${state.snoozesLeft}x)")
                }
            } else {
                Text("Jatah tunda habis. Kasih bukti, atau tulis alasannya.", color = MaterialTheme.colorScheme.error)
            }
        }
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
}
