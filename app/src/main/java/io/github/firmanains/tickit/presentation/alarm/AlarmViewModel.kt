package io.github.firmanains.tickit.presentation.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.firmanains.tickit.domain.AlarmPolicy
import io.github.firmanains.tickit.domain.model.HabitToday
import io.github.firmanains.tickit.domain.model.Proof
import io.github.firmanains.tickit.domain.model.TodayStatus
import io.github.firmanains.tickit.domain.repository.PhotoStore
import io.github.firmanains.tickit.domain.usecase.CompleteHabitUseCase
import io.github.firmanains.tickit.domain.usecase.ExcuseUseCase
import io.github.firmanains.tickit.domain.usecase.ObserveTodayUseCase
import io.github.firmanains.tickit.domain.usecase.SnoozeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** pending == null while loading; an empty list means everything is settled, which stops the alarm. */
data class AlarmUiState(val isEndOfDay: Boolean, val snoozesLeft: Int, val pending: List<HabitToday>? = null)

/** One habit's alarm, or (habitId == null) the end-of-day review of everything still open. */
class AlarmViewModel(
    private val habitId: Long?,
    private val snoozesUsed: Int,
    observeToday: ObserveTodayUseCase,
    private val completeHabit: CompleteHabitUseCase,
    private val excuseHabit: ExcuseUseCase,
    private val snoozeAlarm: SnoozeUseCase,
    private val photos: PhotoStore,
) : ViewModel() {
    private val today = LocalDate.now()
    private val initial = AlarmUiState(habitId == null, AlarmPolicy.MAX_SNOOZES - snoozesUsed)

    val uiState: StateFlow<AlarmUiState> = observeToday(today)
        .map { all -> initial.copy(pending = all.filter { it.status == TodayStatus.PENDING && (habitId == null || it.habit.id == habitId) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    fun complete(id: Long, proof: Proof) {
        viewModelScope.launch { completeHabit(id, today, proof) }
    }

    fun excuse(id: Long, reason: String) {
        viewModelScope.launch { excuseHabit(id, today, reason) }
    }

    fun snooze(): Boolean = snoozeAlarm(habitId, snoozesUsed)

    fun newPhotoPath(): String = photos.newPhotoPath()

    fun discardPhoto(path: String) {
        viewModelScope.launch { photos.discard(path) }
    }
}
