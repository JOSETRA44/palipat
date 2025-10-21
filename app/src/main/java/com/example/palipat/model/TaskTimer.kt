package com.example.palipat.model

enum class Mode { Countdown, Stopwatch }

data class TaskTimer(
    val id: Int,
    val name: String,
    val mode: Mode,
    val durationMillis: Long,
    val remainingMillis: Long = durationMillis,
    val elapsedMillis: Long = 0L,
    val isRunning: Boolean = false,
    val lastStartedAt: Long? = null
)
