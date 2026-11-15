package com.aptasks.dto.task

import com.aptasks.model.Task
import com.aptasks.model.User
import com.aptasks.model.Fine

data class DashboardResponse(
    val tasks: List<TaskDashboardDto>,
    val garbageResponsible: UserSummaryDto?
)

data class TaskDashboardDto(
    val id: Long,
    val title: String,
    val dueDate: String,
    val assignedTo: String,
    val isCompleted: Boolean
)

data class UserSummaryDto(
    val id: Long,
    val name: String
)