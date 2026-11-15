package com.aptasks.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/** Multa registrada. `amount` usa BigDecimal porque a coluna é NUMERIC(10, 2). */
data class Fine(
    val id: UUID = UUID.randomUUID(),
    val timestamp: LocalDateTime,
    val amount: BigDecimal,
    val sender: UUID,
    val receiver: UUID,
    val isPaid: Boolean = false
)
