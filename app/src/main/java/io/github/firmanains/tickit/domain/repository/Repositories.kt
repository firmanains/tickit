package io.github.firmanains.tickit.domain.repository

import io.github.firmanains.tickit.domain.model.CheckIn
import io.github.firmanains.tickit.domain.model.Habit
import io.github.firmanains.tickit.domain.model.ShameEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeCheckIns(): Flow<List<CheckIn>>
    fun observeShame(): Flow<List<ShameEntry>>
    suspend fun activeHabits(): List<Habit>
    suspend fun add(name: String, reminder: LocalTime?, colorIndex: Int, createdOn: LocalDate): Habit

    /** Soft delete: the habit disappears but its Wall of Shame history stays. */
    suspend fun archive(id: Long)

    /** Replaces that day's check-in; a photo it no longer references is deleted. */
    suspend fun setCheckIn(checkIn: CheckIn)

    /** Removes that day's check-in and its photo. */
    suspend fun clearCheckIn(habitId: Long, date: LocalDate)
}

interface SettingsRepository {
    var endOfDay: LocalTime
}

/** Where proof photos are kept. Paths are opaque to the domain. */
interface PhotoStore {
    fun newPhotoPath(): String
    suspend fun discard(path: String)
}

/** Platform alarm scheduling. habitId == null means the end-of-day review alarm. */
interface ReminderScheduler {
    fun scheduleHabit(habit: Habit)
    fun cancelHabit(habitId: Long)
    fun scheduleSnooze(habitId: Long?, snoozesUsed: Int)
    fun scheduleEndOfDay(time: LocalTime)
}
