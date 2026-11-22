package io.github.firmanains.tickit.di

import android.content.Context
import androidx.room.Room
import io.github.firmanains.tickit.alarm.AlarmNotifier
import io.github.firmanains.tickit.alarm.AndroidReminderScheduler
import io.github.firmanains.tickit.data.local.MIGRATION_1_2
import io.github.firmanains.tickit.data.local.TickitDatabase
import io.github.firmanains.tickit.data.photo.LocalPhotoStore
import io.github.firmanains.tickit.data.repository.PrefsSettingsRepository
import io.github.firmanains.tickit.data.repository.RoomHabitRepository
import io.github.firmanains.tickit.domain.repository.HabitRepository
import io.github.firmanains.tickit.domain.repository.ReminderScheduler
import io.github.firmanains.tickit.domain.repository.SettingsRepository
import io.github.firmanains.tickit.domain.usecase.AddHabitUseCase
import io.github.firmanains.tickit.domain.usecase.CompleteHabitUseCase
import io.github.firmanains.tickit.domain.usecase.DeleteHabitUseCase
import io.github.firmanains.tickit.domain.usecase.ExcuseUseCase
import io.github.firmanains.tickit.domain.usecase.ObserveHabitDetailUseCase
import io.github.firmanains.tickit.domain.usecase.ObserveTodayUseCase
import io.github.firmanains.tickit.domain.usecase.RescheduleAllUseCase
import io.github.firmanains.tickit.domain.usecase.SetEndOfDayUseCase
import io.github.firmanains.tickit.domain.usecase.SnoozeUseCase
import io.github.firmanains.tickit.domain.usecase.UndoCheckInUseCase
import io.github.firmanains.tickit.presentation.alarm.AlarmViewModel
import io.github.firmanains.tickit.presentation.habit.HabitDetailViewModel
import io.github.firmanains.tickit.presentation.home.HomeViewModel
import io.github.firmanains.tickit.presentation.shame.ShameViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual DI: one instance per process, wired once. Small enough that Hilt would cost more than it saves. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val db = Room.databaseBuilder(context, TickitDatabase::class.java, "tickit.db").addMigrations(MIGRATION_1_2).build()
    private val photos = LocalPhotoStore(context)
    private val habits: HabitRepository = RoomHabitRepository(db.habitDao(), photos)
    private val settings: SettingsRepository = PrefsSettingsRepository(context)
    private val scheduler: ReminderScheduler = AndroidReminderScheduler(context)
    val notifier = AlarmNotifier(context)

    val observeToday = ObserveTodayUseCase(habits)
    val snooze = SnoozeUseCase(scheduler)
    val rescheduleAll = RescheduleAllUseCase(habits, settings, scheduler)
    private val complete = CompleteHabitUseCase(habits)
    private val excuse = ExcuseUseCase(habits)

    fun homeViewModel() = HomeViewModel(
        observeToday, AddHabitUseCase(habits, scheduler), DeleteHabitUseCase(habits, scheduler), complete,
        UndoCheckInUseCase(habits), excuse, SetEndOfDayUseCase(settings, scheduler), rescheduleAll, settings, photos,
    )

    fun habitDetailViewModel(habitId: Long) = HabitDetailViewModel(habitId, ObserveHabitDetailUseCase(habits))

    fun shameViewModel() = ShameViewModel(habits)

    fun alarmViewModel(habitId: Long?, snoozesUsed: Int) =
        AlarmViewModel(habitId, snoozesUsed, observeToday, complete, excuse, snooze, photos)
}
