package com.aptasks.dto.fine

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/** Multa vista a partir de um morador: `direction` diz se ele é o remetente ou o destinatário. */
data class FineResponse(
    val id: UUID,
    val timestamp: LocalDateTime,
    val amount: BigDecimal,
    val senderId: UUID,
    val senderName: String,
    val receiverId: UUID,
    val receiverName: String,
    val isPaid: Boolean,
    val direction: String
)
