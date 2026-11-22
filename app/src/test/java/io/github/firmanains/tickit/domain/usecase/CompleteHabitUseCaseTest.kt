package io.github.firmanains.tickit.domain.usecase

import io.github.firmanains.tickit.domain.model.CheckIn
import io.github.firmanains.tickit.domain.model.Habit
import io.github.firmanains.tickit.domain.model.Proof
import io.github.firmanains.tickit.domain.model.ShameEntry
import io.github.firmanains.tickit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CompleteHabitUseCaseTest {
    private val saved = mutableListOf<CheckIn>()
    private val repo = object : HabitRepository {
        override fun observeHabits() = emptyFlow<List<Habit>>()
        override fun observeCheckIns() = emptyFlow<List<CheckIn>>()
        override fun observeShame() = emptyFlow<List<ShameEntry>>()
        override suspend fun activeHabits() = emptyList<Habit>()
        override suspend fun add(name: String, reminder: LocalTime?, colorIndex: Int, createdOn: LocalDate): Habit = error("unused")
        override suspend fun archive(id: Long) = Unit
        override suspend fun setCheckIn(checkIn: CheckIn) { saved += checkIn }
        override suspend fun clearCheckIn(habitId: Long, date: LocalDate) = Unit
    }
    private val day = LocalDate.of(2026, 11, 23)

    @Test fun tickStoresTrimmedNoteAndPhoto() = runBlocking {
        CompleteHabitUseCase(repo)(1, day, Proof("  lari 3 km ", "/proofs/a.jpg"))
        assertEquals(listOf(CheckIn(1, day, done = true, note = "lari 3 km", photoPath = "/proofs/a.jpg")), saved)
    }

    @Test fun tickWithoutNoteIsRejected() = runBlocking {
        val result = runCatching { CompleteHabitUseCase(repo)(1, day, Proof("   ", "/proofs/a.jpg")) }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(saved.isEmpty())
    }
}
