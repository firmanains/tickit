package io.github.firmanains.tickit.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.firmanains.tickit.alarm.AlarmIntents.ACTION_RING
import io.github.firmanains.tickit.alarm.AlarmIntents.ACTION_RING_AGAIN
import io.github.firmanains.tickit.domain.AlarmPolicy
import io.github.firmanains.tickit.domain.model.Habit
import io.github.firmanains.tickit.domain.nextTrigger
import io.github.firmanains.tickit.domain.repository.ReminderScheduler
import io.github.firmanains.tickit.presentation.MainActivity
import java.time.LocalTime
import java.time.ZonedDateTime

class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    override fun scheduleHabit(habit: Habit) {
        val time = habit.reminder ?: return cancelHabit(habit.id)
        set(AlarmIntents.receiver(context, ACTION_RING, habit.id, 0), nextTrigger(ZonedDateTime.now(), time))
    }

    override fun cancelHabit(habitId: Long) {
        alarms.cancel(AlarmIntents.receiver(context, ACTION_RING, habitId, 0))
        alarms.cancel(AlarmIntents.receiver(context, ACTION_RING_AGAIN, habitId, 0))
    }

    override fun scheduleSnooze(habitId: Long?, snoozesUsed: Int) = set(
        AlarmIntents.receiver(context, ACTION_RING_AGAIN, habitId, snoozesUsed),
        ZonedDateTime.now().plusMinutes(AlarmPolicy.SNOOZE_MINUTES),
    )

    override fun scheduleEndOfDay(time: LocalTime) =
        set(AlarmIntents.receiver(context, ACTION_RING, null, 0), nextTrigger(ZonedDateTime.now(), time))

    private fun set(operation: PendingIntent, at: ZonedDateTime) {
        val millis = at.toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            // setAlarmClock = exact, survives Doze, and shows the alarm icon like a real alarm clock.
            val show = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
            )
            alarms.setAlarmClock(AlarmManager.AlarmClockInfo(millis, show), operation)
        } else {
            // Exact-alarm permission denied: still rings, but Android may delay it. The home screen asks for it.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, operation)
        }
    }
}
