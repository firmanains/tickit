package io.github.firmanains.tickit.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.firmanains.tickit.presentation.alarm.AlarmActivity

/** Every PendingIntent the alarm flow uses. habitId == null is the end-of-day review. */
object AlarmIntents {
    const val ACTION_RING = "io.github.firmanains.tickit.RING" // daily alarm
    const val ACTION_RING_AGAIN = "io.github.firmanains.tickit.RING_AGAIN" // snoozed alarm
    const val ACTION_SNOOZE = "io.github.firmanains.tickit.SNOOZE"

    private const val EXTRA_HABIT = "habit"
    private const val EXTRA_SNOOZES = "snoozes"
    private const val END_OF_DAY = -1L
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun receiver(context: Context, action: String, habitId: Long?, snoozesUsed: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0, Intent(context, AlarmReceiver::class.java).setAction(action).target(habitId, snoozesUsed), FLAGS,
        )

    fun activity(context: Context, habitId: Long?, snoozesUsed: Int): PendingIntent =
        PendingIntent.getActivity(context, 0, Intent(context, AlarmActivity::class.java).target(habitId, snoozesUsed), FLAGS)

    fun habitId(intent: Intent): Long? = intent.getLongExtra(EXTRA_HABIT, END_OF_DAY).takeIf { it != END_OF_DAY }

    fun snoozesUsed(intent: Intent): Int = intent.getIntExtra(EXTRA_SNOOZES, 0)

    // The data URI makes PendingIntents distinct per habit (extras don't count for equality).
    private fun Intent.target(habitId: Long?, snoozesUsed: Int): Intent =
        setData(Uri.parse("tickit://target/${habitId ?: "eod"}"))
            .putExtra(EXTRA_HABIT, habitId ?: END_OF_DAY)
            .putExtra(EXTRA_SNOOZES, snoozesUsed)
}
