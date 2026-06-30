package com.aptasks.dto

import com.aptasks.model.Task

data class DashboardResponse(
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val recentTasks: List<Task>
)