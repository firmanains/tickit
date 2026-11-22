package io.github.firmanains.tickit.presentation.alarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.firmanains.tickit.alarm.AlarmIntents
import io.github.firmanains.tickit.container
import io.github.firmanains.tickit.domain.AlarmPolicy
import io.github.firmanains.tickit.domain.model.Habit
import io.github.firmanains.tickit.presentation.TickitTheme
import io.github.firmanains.tickit.presentation.common.ProofDialog
import io.github.firmanains.tickit.presentation.common.ReasonDialog
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

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(now, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                if (state.isEndOfDay) "Review akhir hari" else pending.firstOrNull()?.habit?.name.orEmpty(),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                if (state.isEndOfDay) "Yang ini belum dibereskan. Kasih bukti, atau tulis kenapa nggak jalan."
                else "Kerjakan sekarang dan kasih bukti, tunda, atau tulis kenapa nggak jalan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyColumn(Modifier.weight(1f).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                items(pending, key = { it.habit.id }) { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.isEndOfDay) Text(item.habit.name, style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { proofFor = item.habit }) { Text("Selesai, isi bukti") }
                            OutlinedButton(onClick = { excusing = item.habit }) { Text("Nggak bisa") }
                        }
                    }
                }
            }
            if (state.snoozesLeft > 0) {
                OutlinedButton(onClick = { if (vm.snooze()) onStop() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Tunda ${AlarmPolicy.SNOOZE_MINUTES} menit (sisa ${state.snoozesLeft}x)")
                }
            } else {
                Text("Jatah tunda habis. Kasih bukti, atau tulis alasannya.", color = MaterialTheme.colorScheme.error)
            }
        }
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
}
