package io.github.firmanains.tickit.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val reminderMinute: Int?, // minute of day, null = no alarm
    val colorIndex: Int,
    val createdEpochDay: Long,
    val archived: Boolean = false,
)

@Entity(
    tableName = "check_ins",
    primaryKeys = ["habitId", "epochDay"],
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"])],
)
data class CheckInEntity(
    val habitId: Long,
    val epochDay: Long,
    val done: Boolean,
    val reason: String?, // set when skipped
    val note: String?, // proof when done
    val photoPath: String?,
)

data class ShameRow(val name: String, val epochDay: Long, val reason: String?)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY id")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY id")
    suspend fun activeHabits(): List<HabitEntity>

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Query("UPDATE habits SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    // shortcut: loads all history to draw tiles and streaks; fine for one person's habits, window by date if it gets slow
    @Query("SELECT c.* FROM check_ins c JOIN habits h ON h.id = c.habitId WHERE h.archived = 0")
    fun observeCheckIns(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun checkIn(habitId: Long, epochDay: Long): CheckInEntity?

    @Upsert
    suspend fun upsert(checkIn: CheckInEntity)

    @Query("DELETE FROM check_ins WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun delete(habitId: Long, epochDay: Long)

    // No archived filter: deleting a habit doesn't wipe your shame.
    @Query(
        "SELECT h.name AS name, c.epochDay AS epochDay, c.reason AS reason FROM check_ins c " +
            "JOIN habits h ON h.id = c.habitId WHERE c.done = 0 ORDER BY c.epochDay DESC, h.name",
    )
    fun observeShame(): Flow<List<ShameRow>>
}

@Database(entities = [HabitEntity::class, CheckInEntity::class], version = 1, exportSchema = false)
abstract class TickitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
}
