package com.aptasks.model

import java.time.Instant
import java.time.LocalDate

/** Prazo prorrogado de uma semana de tarefas. A rotação seguinte só acontece depois de [extendedUntil]. */
data class TaskWeekExtension(
    val weekStart: LocalDate,
    val extendedUntil: Instant
)
