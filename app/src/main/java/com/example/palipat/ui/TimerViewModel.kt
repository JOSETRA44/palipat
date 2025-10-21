package com.example.palipat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.palipat.model.Mode
import com.example.palipat.model.TaskTimer
import com.example.palipat.service.StorageService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Data class to hold calculated statistics for a timer activity
data class ActivityStats(val avg: Long = 0L, val max: Long = 0L, val min: Long = 0L)

class TimerViewModel(private val storage: StorageService) : ViewModel() {

    val items: StateFlow<List<TaskTimer>> = storage.observe()
        .map { records ->
            records.map { record ->
                val now = System.currentTimeMillis()
                val recMode = if (record.mode == "Stopwatch") Mode.Stopwatch else Mode.Countdown
                val (newElapsed, newRemaining, stillRunning) = if (record.isRunning && record.lastStartedAt != null) {
                    val delta = (now - record.lastStartedAt).coerceAtLeast(0L)
                    when (recMode) {
                        Mode.Stopwatch -> Triple(record.elapsedMillis + delta, record.remainingMillis, true)
                        Mode.Countdown -> {
                            val rem = (record.remainingMillis - delta).coerceAtLeast(0L)
                            Triple(record.elapsedMillis, rem, rem > 0L)
                        }
                    }
                } else Triple(record.elapsedMillis, record.remainingMillis, false)

                TaskTimer(
                    id = record.id,
                    name = record.name,
                    mode = recMode,
                    durationMillis = record.durationMillis,
                    remainingMillis = newRemaining,
                    elapsedMillis = newElapsed,
                    isRunning = stillRunning,
                    lastStartedAt = if (stillRunning) record.lastStartedAt else null
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Combine the timers and historical sessions to calculate statistics.
    val activityStats: StateFlow<Map<String, ActivityStats>> =
        combine(items, storage.observeSessions()) { timers, sessions ->
            val allDurationsByName = sessions.groupBy { it.name }
                .mapValues { entry -> entry.value.map { it.durationMillis }.toMutableList() }
                .toMutableMap()

            val now = System.currentTimeMillis()
            timers.filter { it.isRunning && it.lastStartedAt != null }.forEach { timer ->
                val currentSessionDuration = now - timer.lastStartedAt!!
                allDurationsByName.getOrPut(timer.name) { mutableListOf() }.add(currentSessionDuration)
            }

            allDurationsByName.mapValues { (_, durations) ->
                val validDurations = durations.filter { it > 0 }
                if (validDurations.isNotEmpty()) {
                    ActivityStats(
                        avg = validDurations.average().toLong(),
                        max = validDurations.maxOrNull() ?: 0L,
                        min = validDurations.minOrNull() ?: 0L
                    )
                } else {
                    ActivityStats()
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())


    fun updateItem(id: Int, transform: (TaskTimer) -> TaskTimer) {
        val currentList = items.value
        val updatedList = currentList.map { if (it.id == id) transform(it) else it }
        saveTimers(updatedList)
    }

    fun addTask(name: String, minutes: Int, mode: Mode) {
        val currentList = items.value
        val nextId = (currentList.maxOfOrNull { it.id } ?: 0) + 1
        val duration = (minutes.coerceAtLeast(0)) * 60_000L
        val finalName = name.ifBlank { "Actividad $nextId" }
        val now = System.currentTimeMillis()
        val item = if (mode == Mode.Countdown) {
            TaskTimer(
                id = nextId,
                name = finalName,
                mode = Mode.Countdown,
                durationMillis = duration,
                remainingMillis = duration,
                elapsedMillis = 0L,
                isRunning = true,
                lastStartedAt = now
            )
        } else {
            TaskTimer(
                id = nextId,
                name = finalName,
                mode = Mode.Stopwatch,
                durationMillis = 0L,
                remainingMillis = 0L,
                elapsedMillis = 0L,
                isRunning = true,
                lastStartedAt = now
            )
        }
        saveTimers(currentList + item)
    }

    fun deleteTask(id: Int) {
        val currentList = items.value
        val updatedList = currentList.filterNot { it.id == id }
        saveTimers(updatedList)
    }

    private fun saveTimers(timers: List<TaskTimer>) {
        viewModelScope.launch {
            val records = timers.map { t ->
                StorageService.TimerRecord(
                    id = t.id,
                    name = t.name,
                    mode = if (t.mode == Mode.Stopwatch) "Stopwatch" else "Countdown",
                    durationMillis = t.durationMillis,
                    remainingMillis = t.remainingMillis,
                    elapsedMillis = t.elapsedMillis,
                    isRunning = t.isRunning,
                    lastStartedAt = t.lastStartedAt
                )
            }
            storage.save(records)
        }
    }

    fun appendSession(session: StorageService.SessionLog) {
        viewModelScope.launch {
            storage.appendSession(session)
        }
    }
}
