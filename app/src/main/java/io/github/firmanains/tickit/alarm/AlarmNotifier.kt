package io.github.firmanains.tickit.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import io.github.firmanains.tickit.R
import io.github.firmanains.tickit.alarm.AlarmIntents.ACTION_SNOOZE
import io.github.firmanains.tickit.domain.AlarmPolicy
import io.github.firmanains.tickit.domain.model.HabitToday
import java.util.concurrent.TimeUnit

/** Rings like an alarm: alarm sound on loop (FLAG_INSISTENT) + full-screen AlarmActivity, until handled. */
class AlarmNotifier(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Alarm habit", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alarm pengingat habit dan review akhir hari"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 600)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun ring(habitId: Long?, pending: List<HabitToday>, snoozesUsed: Int) {
        val open = AlarmIntents.activity(context, habitId, snoozesUsed)
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle(if (habitId == null) "Review akhir hari" else pending.first().habit.name)
            .setContentText(
                if (habitId == null) "${pending.size} habit belum di-tick. Tick atau kasih alasan."
                else "Waktunya! Kerjain, tunda, atau kasih alasan.",
            )
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setTimeoutAfter(TimeUnit.MINUTES.toMillis(AlarmPolicy.RING_TIMEOUT_MINUTES))
        if (habitId != null) {
            builder.addAction(action("Selesai, isi bukti", open)) // proof needs the screen, so this opens it
        }
        if (snoozesUsed < AlarmPolicy.MAX_SNOOZES) {
            val label = "Tunda ${AlarmPolicy.SNOOZE_MINUTES} mnt"
            builder.addAction(action(label, AlarmIntents.receiver(context, ACTION_SNOOZE, habitId, snoozesUsed)))
        }
        val notification = builder.build().apply { flags = flags or Notification.FLAG_INSISTENT }
        manager.notify(notificationId(habitId), notification)
    }

    fun stop(habitId: Long?) = manager.cancel(notificationId(habitId))

    private fun action(title: String, intent: android.app.PendingIntent) =
        Notification.Action.Builder(null as Icon?, title, intent).build()

    private fun notificationId(habitId: Long?) = habitId?.toInt() ?: 0 // habit ids start at 1

    private companion object {
        const val CHANNEL_ID = "alarm"
    }
}
