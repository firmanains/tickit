package io.github.firmanains.tickit.domain

import io.github.firmanains.tickit.domain.model.CheckIn
import io.github.firmanains.tickit.domain.model.DayStatus
import io.github.firmanains.tickit.domain.model.HabitStats
import io.github.firmanains.tickit.domain.model.HabitToday
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object AlarmPolicy {
    const val SNOOZE_MINUTES = 10L
    const val MAX_SNOOZES = 3
    const val RING_TIMEOUT_MINUTES = 10L
}

/** Next moment [time] occurs strictly after [now], so an alarm firing on time queues tomorrow's. */
fun nextTrigger(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
    val today = now.toLocalDate().atTime(time).atZone(now.zone)
    return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
}

/** Consecutive ticked days ending today, or ending yesterday while today is still open. */
fun streak(doneDates: Set<LocalDate>, today: LocalDate): Int {
    var day = if (today in doneDates) today else today.minusDays(1)
    var count = 0
    while (day in doneDates) {
        count++
        day = day.minusDays(1)
    }
    return count
}

/** GitHub-style layout: Monday-first week columns, oldest first, the last one holding [today]. */
fun weekColumns(today: LocalDate, weeks: Int): List<List<LocalDate>> {
    val thisMonday = today.with(DayOfWeek.MONDAY)
    return (weeks - 1 downTo 0).map { back ->
        val monday = thisMonday.minusWeeks(back.toLong())
        (0L..6L).map(monday::plusDays)
    }
}

fun dayStatus(history: Map<LocalDate, CheckIn>, createdOn: LocalDate, today: LocalDate, date: LocalDate): DayStatus {
    val checkIn = history[date]
    return when {
        checkIn != null -> if (checkIn.done) DayStatus.DONE else DayStatus.EXCUSED
        !date.isBefore(today) || date.isBefore(createdOn) -> DayStatus.NONE // today is still open
        else -> DayStatus.MISSED
    }
}

/** Share of the habits that existed on [date] and were ticked that day; null when none existed yet. */
fun dayProgress(habits: List<HabitToday>, date: LocalDate): Float? {
    val existing = habits.filter { !it.habit.createdOn.isAfter(date) }
    if (existing.isEmpty()) return null
    return existing.count { it.history[date]?.done == true }.toFloat() / existing.size
}

fun stats(history: Map<LocalDate, CheckIn>, createdOn: LocalDate, today: LocalDate): HabitStats {
    val done = history.values.filter { it.done }.map { it.date }.toSortedSet()
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    for (day in done) {
        run = if (previous != null && day == previous.plusDays(1)) run + 1 else 1
        best = maxOf(best, run)
        previous = day
    }
    // Today only counts once it's been judged, so an open morning doesn't drag the score down.
    val lastJudged = if (history[today] != null) today else today.minusDays(1)
    val days = ChronoUnit.DAYS.between(createdOn, lastJudged) + 1
    val doneInRange = done.count { !it.isBefore(createdOn) && !it.isAfter(lastJudged) }
    val percent = if (days <= 0) 0 else (doneInRange * 100 / days).toInt()
    return HabitStats(streak(done, today), best, percent)
}
