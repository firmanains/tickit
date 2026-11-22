package io.github.firmanains.tickit.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Habit(
    val id: Long,
    val name: String,
    val reminder: LocalTime?,
    val colorIndex: Int,
    val createdOn: LocalDate,
)

/** One per habit per day: ticked off with proof, or skipped with a reason (a Wall of Shame entry). */
data class CheckIn(
    val habitId: Long,
    val date: LocalDate,
    val done: Boolean,
    val reason: String? = null,
    val note: String? = null,
    val photoPath: String? = null,
)

/** What a tick has to come with: what you did, optionally a photo of it. */
data class Proof(val note: String, val photoPath: String?)

enum class TodayStatus { PENDING, DONE, EXCUSED }

/** One tile in the progress grid. NONE = nothing to judge yet (before the habit existed, today, or the future). */
enum class DayStatus { DONE, EXCUSED, MISSED, NONE }

data class HabitToday(
    val habit: Habit,
    val status: TodayStatus,
    val streak: Int,
    val reason: String?,
    val history: Map<LocalDate, CheckIn>,
)

data class HabitStats(val currentStreak: Int, val bestStreak: Int, val consistencyPercent: Int)

data class HabitDetail(
    val habit: Habit,
    val stats: HabitStats,
    val history: Map<LocalDate, CheckIn>,
    val journal: List<CheckIn>, // newest first
)

data class ShameEntry(val habitName: String, val date: LocalDate, val reason: String)
