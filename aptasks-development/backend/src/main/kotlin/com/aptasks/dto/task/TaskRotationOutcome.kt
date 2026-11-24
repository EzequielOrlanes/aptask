package com.aptasks.dto.task

import java.util.UUID

/** Linha do histórico de rotação com o resultado da semana. [isCompleted] é null quando não há registro. */
data class TaskRotationOutcome(
    val taskId: UUID?,
    val taskDescription: String,
    val userId: UUID,
    val userName: String,
    val isCompleted: Boolean?
)
