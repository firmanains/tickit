package io.github.firmanains.tickit.data.repository

import android.content.Context
import io.github.firmanains.tickit.data.local.CheckInEntity
import io.github.firmanains.tickit.data.local.HabitDao
import io.github.firmanains.tickit.data.local.HabitEntity
import io.github.firmanains.tickit.data.photo.LocalPhotoStore
import io.github.firmanains.tickit.domain.model.CheckIn
import io.github.firmanains.tickit.domain.model.Habit
import io.github.firmanains.tickit.domain.model.ShameEntry
import io.github.firmanains.tickit.domain.repository.HabitRepository
import io.github.firmanains.tickit.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

class RoomHabitRepository(private val dao: HabitDao, private val photos: LocalPhotoStore) : HabitRepository {
    override fun observeHabits() = dao.observeHabits().map { rows -> rows.map { it.toDomain() } }

    override fun observeCheckIns() = dao.observeCheckIns().map { rows -> rows.map { it.toDomain() } }

    override fun observeShame() = dao.observeShame().map { rows ->
        rows.map { ShameEntry(it.name, LocalDate.ofEpochDay(it.epochDay), it.reason.orEmpty()) }
    }

    override suspend fun activeHabits() = dao.activeHabits().map { it.toDomain() }

    override suspend fun add(name: String, reminder: LocalTime?, colorIndex: Int, createdOn: LocalDate): Habit {
        val entity = HabitEntity(
            name = name,
            reminderMinute = reminder?.let { it.hour * 60 + it.minute },
            colorIndex = colorIndex,
            createdEpochDay = createdOn.toEpochDay(),
        )
        return entity.copy(id = dao.insert(entity)).toDomain()
    }

    override suspend fun archive(id: Long) = dao.archive(id)

    override suspend fun setCheckIn(checkIn: CheckIn) {
        val day = checkIn.date.toEpochDay()
        val old = dao.checkIn(checkIn.habitId, day)
        checkIn.photoPath?.takeIf { it != old?.photoPath }?.let { photos.shrink(it) }
        dao.upsert(
            CheckInEntity(checkIn.habitId, day, checkIn.done, checkIn.reason, checkIn.note, checkIn.photoPath),
        )
        old?.photoPath?.takeIf { it != checkIn.photoPath }?.let { photos.discard(it) }
    }

    override suspend fun clearCheckIn(habitId: Long, date: LocalDate) {
        val old = dao.checkIn(habitId, date.toEpochDay()) ?: return
        dao.delete(habitId, date.toEpochDay())
        old.photoPath?.let { photos.discard(it) }
    }
}

private fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    reminder = reminderMinute?.let { LocalTime.of(it / 60, it % 60) },
    colorIndex = colorIndex,
    createdOn = LocalDate.ofEpochDay(createdEpochDay),
)

private fun CheckInEntity.toDomain() = CheckIn(habitId, LocalDate.ofEpochDay(epochDay), done, reason, note, photoPath)

class PrefsSettingsRepository(context: Context) : SettingsRepository {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    override var endOfDay: LocalTime
        get() = prefs.getInt(KEY_END_OF_DAY, 21 * 60).let { LocalTime.of(it / 60, it % 60) }
        set(value) = prefs.edit().putInt(KEY_END_OF_DAY, value.hour * 60 + value.minute).apply()

    private companion object {
        const val KEY_END_OF_DAY = "end_of_day_minute"
    }
}
