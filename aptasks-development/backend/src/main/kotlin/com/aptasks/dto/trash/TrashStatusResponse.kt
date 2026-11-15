package com.aptasks.dto.trash

import com.aptasks.dto.ResidentResponse
import java.time.Instant

data class TrashStatusResponse(
    val responsible: ResidentResponse?,
    val fillLevel: String,
    val isFull: Boolean,
    val reportedAt: Instant?,
    val reportedBy: ResidentResponse?
)
