package com.aptasks.dto.task

import com.aptasks.model.Task
import com.aptasks.model.User
import com.aptasks.model.Fine

data class DashboardResponse(
    val tasks: List<TaskDashboardDto>,
    val garbageResponsible: UserSummaryDto?,
    val notices: List<NoticeDto> // Se optar por criar a entidade Notice futuramente, ou gerenciar via Task/Fine
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

data class NoticeDto(
    val id: Long,
    val content: String,
    val author: String,
    val createdAt: String
)