package com.aptasks.dto.fine

import java.time.LocalDateTime

data class FineRequest(
    val amount: Double,
    val sender: String,
    val receiver: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/** Corpo de PATCH /api/fines/{id}/paid — vazio, mantido para contrato explícito. */
data class FinePaidRequest(val isPaid: Boolean = true)
