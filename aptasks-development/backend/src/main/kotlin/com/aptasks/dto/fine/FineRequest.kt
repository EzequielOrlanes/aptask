package com.aptasks.dto.fine

import java.time.LocalDateTime

data class FineRequest(
    val amount: Double,
    val sender: String,
    val receiver: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)