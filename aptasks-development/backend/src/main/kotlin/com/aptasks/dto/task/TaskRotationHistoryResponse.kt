package com.aptasks.dto.task

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class TaskRotationHistoryResponse(
    val taskId: UUID?,
    val taskDescription: String,
    val userId: UUID,
    val userName: String,
    val weekStart: LocalDate,
    val assignedAt: Instant
)
