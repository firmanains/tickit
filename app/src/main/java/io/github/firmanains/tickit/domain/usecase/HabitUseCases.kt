package io.github.firmanains.tickit.domain.usecase

import io.github.firmanains.tickit.domain.AlarmPolicy
import io.github.firmanains.tickit.domain.model.CheckIn
import io.github.firmanains.tickit.domain.model.HabitDetail
import io.github.firmanains.tickit.domain.model.HabitToday
import io.github.firmanains.tickit.domain.model.Proof
import io.github.firmanains.tickit.domain.model.TodayStatus
import io.github.firmanains.tickit.domain.repository.HabitRepository
import io.github.firmanains.tickit.domain.repository.ReminderScheduler
import io.github.firmanains.tickit.domain.repository.SettingsRepository
import io.github.firmanains.tickit.domain.stats
import io.github.firmanains.tickit.domain.streak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.LocalTime

class ObserveTodayUseCase(private val repo: HabitRepository) {
    operator fun invoke(today: LocalDate): Flow<List<HabitToday>> =
        combine(repo.observeHabits(), repo.observeCheckIns()) { habits, checkIns ->
            val byHabit = checkIns.groupBy { it.habitId }
            habits.map { habit ->
                val history = byHabit[habit.id].orEmpty().associateBy { it.date }
                val todays = history[today]
                HabitToday(
                    habit = habit,
                    status = when {
                        todays == null -> TodayStatus.PENDING
                        todays.done -> TodayStatus.DONE
                        else -> TodayStatus.EXCUSED
                    },
                    streak = streak(history.values.filter { it.done }.mapTo(HashSet()) { it.date }, today),
                    reason = todays?.reason,
                    history = history,
                )
            }
        }
}

class ObserveHabitDetailUseCase(private val repo: HabitRepository) {
    operator fun invoke(habitId: Long, today: LocalDate): Flow<HabitDetail?> =
        combine(repo.observeHabits(), repo.observeCheckIns()) { habits, checkIns ->
            val habit = habits.find { it.id == habitId } ?: return@combine null
            val mine = checkIns.filter { it.habitId == habitId }
            val history = mine.associateBy { it.date }
            HabitDetail(habit, stats(history, habit.createdOn, today), history, mine.sortedByDescending { it.date })
        }
}

class AddHabitUseCase(private val repo: HabitRepository, private val scheduler: ReminderScheduler) {
    suspend operator fun invoke(name: String, emoji: String, reminder: LocalTime?, colorIndex: Int, today: LocalDate) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Habit name is blank" }
        scheduler.scheduleHabit(repo.add(clean, emoji, reminder, colorIndex, today))
    }
}

class DeleteHabitUseCase(private val repo: HabitRepository, private val scheduler: ReminderScheduler) {
    suspend operator fun invoke(habitId: Long) {
        repo.archive(habitId)
        scheduler.cancelHabit(habitId)
    }
}

/** A tick only counts with proof: a note of what you did, plus an optional photo. */
class CompleteHabitUseCase(private val repo: HabitRepository) {
    suspend operator fun invoke(habitId: Long, date: LocalDate, proof: Proof) {
        val note = proof.note.trim()
        require(note.isNotEmpty()) { "A tick needs a note as proof" }
        repo.setCheckIn(CheckIn(habitId, date, done = true, note = note, photoPath = proof.photoPath))
    }
}

class UndoCheckInUseCase(private val repo: HabitRepository) {
    suspend operator fun invoke(habitId: Long, date: LocalDate) = repo.clearCheckIn(habitId, date)
}

/** "No progress" must come with a reason; it lands on the Wall of Shame. */
class ExcuseUseCase(private val repo: HabitRepository) {
    suspend operator fun invoke(habitId: Long, date: LocalDate, reason: String) {
        val clean = reason.trim()
        require(clean.isNotEmpty()) { "A shame entry needs a reason" }
        repo.setCheckIn(CheckIn(habitId, date, done = false, reason = clean))
    }
}

/** Returns false once the snooze budget is spent: then it's tick it or explain yourself. */
class SnoozeUseCase(private val scheduler: ReminderScheduler) {
    operator fun invoke(habitId: Long?, snoozesUsed: Int): Boolean {
        if (snoozesUsed >= AlarmPolicy.MAX_SNOOZES) return false
        scheduler.scheduleSnooze(habitId, snoozesUsed + 1)
        return true
    }
}

class RescheduleAllUseCase(
    private val repo: HabitRepository,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke() {
        repo.activeHabits().forEach(scheduler::scheduleHabit)
        scheduler.scheduleEndOfDay(settings.endOfDay)
    }
}

class SetEndOfDayUseCase(private val settings: SettingsRepository, private val scheduler: ReminderScheduler) {
    operator fun invoke(time: LocalTime) {
        settings.endOfDay = time
        scheduler.scheduleEndOfDay(time)
    }
}
