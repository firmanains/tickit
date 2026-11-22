package io.github.firmanains.tickit.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private fun at(h: Int, m: Int) = ZonedDateTime.of(2026, 11, 22, h, m, 0, 0, zone)
    private val seven = LocalTime.of(7, 0)
    private val today = LocalDate.of(2026, 11, 22)
    private fun daysAgo(vararg n: Long) = n.mapTo(HashSet()) { today.minusDays(it) }

    @Test fun laterTodayFiresToday() = assertEquals(at(7, 0), nextTrigger(at(6, 0), seven))

    @Test fun passedTimeFiresTomorrow() = assertEquals(at(7, 0).plusDays(1), nextTrigger(at(8, 0), seven))

    @Test fun firingOnTimeQueuesTomorrow() = assertEquals(at(7, 0).plusDays(1), nextTrigger(at(7, 0), seven))

    @Test fun streakIncludesToday() = assertEquals(3, streak(daysAgo(0, 1, 2), today))

    @Test fun openTodayKeepsYesterdaysStreak() = assertEquals(2, streak(daysAgo(1, 2), today))

    @Test fun gapBreaksStreak() = assertEquals(1, streak(daysAgo(0, 2, 3), today))

    @Test fun missedYesterdayIsZero() = assertEquals(0, streak(daysAgo(2, 3), today))
}
