package io.github.firmanains.tickit.domain

import io.github.firmanains.tickit.domain.model.CheckIn
import io.github.firmanains.tickit.domain.model.DayStatus
import io.github.firmanains.tickit.domain.model.HabitStats
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class TilesTest {
    private val today = LocalDate.of(2026, 11, 25) // a Wednesday
    private val created = LocalDate.of(2026, 11, 16)
    private fun done(vararg daysAgo: Long) = daysAgo.associate { today.minusDays(it) to CheckIn(1, today.minusDays(it), done = true) }
    private fun excused(daysAgo: Long) = today.minusDays(daysAgo).let { mapOf(it to CheckIn(1, it, done = false, reason = "hujan")) }

    @Test fun columnsAreMondayFirstAndEndWithThisWeek() {
        val columns = weekColumns(today, 3)
        assertEquals(3, columns.size)
        assertEquals(LocalDate.of(2026, 11, 9), columns.first().first())
        assertEquals(DayOfWeek.MONDAY, columns.last().first().dayOfWeek)
        assertEquals(LocalDate.of(2026, 11, 29), columns.last().last())
        assert(columns.all { it.size == 7 })
    }

    @Test fun dayStatusJudgesOnlyThePast() {
        val history = done(1) + excused(2)
        fun status(date: LocalDate) = dayStatus(history, created, today, date)
        assertEquals(DayStatus.DONE, status(today.minusDays(1)))
        assertEquals(DayStatus.EXCUSED, status(today.minusDays(2)))
        assertEquals(DayStatus.MISSED, status(today.minusDays(3)))
        assertEquals(DayStatus.NONE, status(today)) // still open
        assertEquals(DayStatus.NONE, status(today.plusDays(1)))
        assertEquals(DayStatus.NONE, status(created.minusDays(1))) // habit didn't exist yet
    }

    @Test fun statsCountBestRunAndIgnoreAnOpenToday() {
        // created 9 days ago; done 8,7,6 (run of 3), 3, 2, 1 (current run of 3); today open
        val history = done(8, 7, 6, 3, 2, 1) + excused(4)
        // judged days: created..yesterday = 9 days, 6 done -> 66%
        assertEquals(HabitStats(currentStreak = 3, bestStreak = 3, consistencyPercent = 66), stats(history, created, today))
    }

    @Test fun statsOnDayOneWithNothingJudgedIsZero() =
        assertEquals(HabitStats(0, 0, 0), stats(emptyMap(), today, today))
}
