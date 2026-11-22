package io.github.firmanains.tickit.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.firmanains.tickit.alarm.AlarmIntents.ACTION_RING
import io.github.firmanains.tickit.alarm.AlarmIntents.ACTION_RING_AGAIN
import io.github.firmanains.tickit.alarm.AlarmIntents.ACTION_SNOOZE
import io.github.firmanains.tickit.container
import io.github.firmanains.tickit.domain.model.TodayStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Alarm fired, or the snooze button on the notification was pressed. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = context.container
        val habitId = AlarmIntents.habitId(intent)
        val snoozesUsed = AlarmIntents.snoozesUsed(intent)
        val today = LocalDate.now()
        val result = goAsync()
        c.appScope.launch {
            try {
                when (intent.action) {
                    ACTION_RING, ACTION_RING_AGAIN -> {
                        if (intent.action == ACTION_RING) c.rescheduleAll() // queue tomorrow's alarms
                        val pending = c.observeToday(today).first()
                            .filter { it.status == TodayStatus.PENDING && (habitId == null || it.habit.id == habitId) }
                        // Already ticked or excused (or deleted): stay quiet.
                        if (pending.isNotEmpty()) c.notifier.ring(habitId, pending, snoozesUsed)
                    }
                    ACTION_SNOOZE -> {
                        c.snooze(habitId, snoozesUsed)
                        c.notifier.stop(habitId)
                    }
                }
            } finally {
                result.finish()
            }
        }
    }
}

/** Android drops alarms on reboot, time/zone change and permission change; put them back. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = context.container
        val result = goAsync()
        c.appScope.launch {
            try {
                c.rescheduleAll()
            } finally {
                result.finish()
            }
        }
    }
}
