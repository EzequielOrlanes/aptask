package com.aptasks.dto.task

import java.time.Instant

/**
 * Novo prazo de uma semana de tarefas. [week] é "current" (semana em vigor, o padrão) ou
 * "previous" (reabre a semana anterior à em vigor, que volta a ser a atual até [until]).
 */
data class ExtendWeekRequest(val until: Instant, val week: String = "current")
