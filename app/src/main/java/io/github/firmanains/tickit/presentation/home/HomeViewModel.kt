package io.github.firmanains.tickit.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.firmanains.tickit.domain.model.HabitToday
import io.github.firmanains.tickit.domain.model.Proof
import io.github.firmanains.tickit.domain.model.TodayStatus
import io.github.firmanains.tickit.domain.repository.PhotoStore
import io.github.firmanains.tickit.domain.repository.SettingsRepository
import io.github.firmanains.tickit.domain.usecase.AddHabitUseCase
import io.github.firmanains.tickit.domain.usecase.CompleteHabitUseCase
import io.github.firmanains.tickit.domain.usecase.DeleteHabitUseCase
import io.github.firmanains.tickit.domain.usecase.ExcuseUseCase
import io.github.firmanains.tickit.domain.usecase.ObserveTodayUseCase
import io.github.firmanains.tickit.domain.usecase.RescheduleAllUseCase
import io.github.firmanains.tickit.domain.usecase.SetEndOfDayUseCase
import io.github.firmanains.tickit.domain.usecase.UndoCheckInUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class HomeUiState(
    val loaded: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val habits: List<HabitToday> = emptyList(),
    val endOfDay: LocalTime = LocalTime.of(21, 0),
) {
    val settled: Int get() = habits.count { it.status != TodayStatus.PENDING }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    observeToday: ObserveTodayUseCase,
    private val addHabit: AddHabitUseCase,
    private val deleteHabit: DeleteHabitUseCase,
    private val completeHabit: CompleteHabitUseCase,
    private val undoCheckIn: UndoCheckInUseCase,
    private val excuseHabit: ExcuseUseCase,
    private val setEndOfDay: SetEndOfDayUseCase,
    rescheduleAll: RescheduleAllUseCase,
    settings: SettingsRepository,
    private val photos: PhotoStore,
) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())
    private val endOfDay = MutableStateFlow(settings.endOfDay)

    val uiState: StateFlow<HomeUiState> =
        combine(today.flatMapLatest { day -> observeToday(day).map { day to it } }, endOfDay) { (day, habits), eod ->
            HomeUiState(loaded = true, today = day, habits = habits, endOfDay = eod)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(endOfDay = settings.endOfDay))

    init {
        // Heals alarms lost to force-stop or a cleared app.
        viewModelScope.launch { rescheduleAll() }
    }

    /** Called on resume so an app left open overnight acts on the right day. */
    fun refreshDay() {
        today.value = LocalDate.now()
    }

    fun add(name: String, reminder: LocalTime?, colorIndex: Int) = launch { addHabit(name, reminder, colorIndex, today.value) }

    fun delete(habitId: Long) = launch { deleteHabit(habitId) }

    fun complete(habitId: Long, proof: Proof) = launch { completeHabit(habitId, today.value, proof) }

    fun undo(habitId: Long) = launch { undoCheckIn(habitId, today.value) }

    fun excuse(habitId: Long, reason: String) = launch { excuseHabit(habitId, today.value, reason) }

    fun changeEndOfDay(time: LocalTime) {
        setEndOfDay(time)
        endOfDay.value = time
    }

    fun newPhotoPath(): String = photos.newPhotoPath()

    fun discardPhoto(path: String) = launch { photos.discard(path) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
