package com.aptasks.dto.task

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Quadro semanal: semana anterior, atual e próxima, nessa ordem. [extendedUntil] vem preenchido
 * quando o prazo da semana atual foi prorrogado e a rotação está adiada.
 */
data class WeeklyBoardResponse(val weeks: List<BoardWeekResponse>, val extendedUntil: Instant? = null)

data class BoardWeekResponse(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    /** "previous", "current" ou "next". */
    val position: String,
    val entries: List<BoardEntryResponse>
)

data class BoardEntryResponse(
    val userId: UUID,
    val userName: String,
    val taskDescription: String,
    /** "done", "missed", "pending", "upcoming" ou "unknown" (semana passada sem registro). */
    val status: String
)
