package com.aptasks.dto.task

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class WeeklyTaskResponse(
    val id: UUID,
    val description: String,
    val userId: UUID,
    val userName: String,
    val weekStart: LocalDate,
    val deadline: Instant?,
    val status: String,
    val decisionRecorded: Boolean,
    val fineApplied: Boolean,
    val fineAmount: Double?
)
