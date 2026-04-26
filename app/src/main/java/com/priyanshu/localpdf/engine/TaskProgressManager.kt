package com.priyanshu.localpdf.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TaskState(
    val isActive: Boolean = false,
    val taskName: String = "",
    val progress: Float = 0f, // 0.0 to 1.0
    val estimatedSecondsRemaining: Long = -1,
    val statusText: String = ""
)

object TaskProgressManager {
    private val _taskState = MutableStateFlow(TaskState())
    val taskState = _taskState.asStateFlow()

    private var startTime: Long = 0
    private var lastUpdateProgress: Float = 0f
    private var lastUpdateTime: Long = 0
    private var smoothedEstimatedTotal: Double = -1.0

    fun startTask(name: String) {
        startTime = System.currentTimeMillis()
        lastUpdateTime = startTime
        lastUpdateProgress = 0f
        smoothedEstimatedTotal = -1.0
        _taskState.value = TaskState(isActive = true, taskName = name, progress = 0f, statusText = "Initializing...")
    }

    fun updateProgress(progress: Float, status: String = "") {
        val now = System.currentTimeMillis()
        val elapsed = (now - startTime) / 1000.0
        
        // Simple smoothing alpha
        val alpha = 0.2
        val currentEstimatedTotal = if (progress > 0.05) elapsed / progress else -1.0
        
        if (currentEstimatedTotal > 0) {
            smoothedEstimatedTotal = if (smoothedEstimatedTotal < 0) {
                currentEstimatedTotal
            } else {
                (alpha * currentEstimatedTotal) + ((1.0 - alpha) * smoothedEstimatedTotal)
            }
        }

        val remaining = if (smoothedEstimatedTotal > 0 && progress < 1.0) {
            (smoothedEstimatedTotal - elapsed).toLong().coerceAtLeast(1)
        } else if (progress >= 1.0) {
            0L
        } else {
            -1L
        }

        lastUpdateProgress = progress
        lastUpdateTime = now

        _taskState.value = _taskState.value.copy(
            progress = progress.coerceIn(0f, 1f),
            statusText = status,
            estimatedSecondsRemaining = remaining
        )
    }

    fun finishTask() {
        _taskState.value = TaskState(isActive = false)
    }
}
