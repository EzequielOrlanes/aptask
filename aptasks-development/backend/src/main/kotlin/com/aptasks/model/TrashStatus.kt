package com.aptasks.model

import java.time.Instant
import java.util.UUID

data class TrashStatus(
    val responsibleUserId: UUID?,
    /** Um de: EMPTY, HALF, FULL (espelha o CHECK de trash_status.fill_level). */
    val fillLevel: String,
    val reportedAt: Instant?,
    val reportedByUserId: UUID?
) {
    val isFull: Boolean get() = fillLevel == "FULL"

    companion object {
        /** Estado usado quando ainda não existe linha em trash_status. */
        val EMPTY = TrashStatus(
            responsibleUserId = null,
            fillLevel = "EMPTY",
            reportedAt = null,
            reportedByUserId = null
        )
    }
}
