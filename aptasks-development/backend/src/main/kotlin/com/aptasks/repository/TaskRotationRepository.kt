package com.aptasks.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Date
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate

@Repository
class TaskRotationRepository(private val jdbcTemplate: JdbcTemplate) {
    fun hasRun(weekStart: LocalDate): Boolean = jdbcTemplate.queryForObject(
        "SELECT EXISTS (SELECT 1 FROM task_rotation_runs WHERE week_start = ?)",
        Boolean::class.java,
        Date.valueOf(weekStart)
    )

    /**
     * Registra a semana uma única vez. A chave primária mais ON CONFLICT
     * protege contra duas instâncias do backend tentando rotacionar juntas.
     */
    fun registerWeekIfAbsent(weekStart: LocalDate, executedAt: Instant): Boolean =
        jdbcTemplate.update(
            """
            INSERT INTO task_rotation_runs (week_start, executed_at)
            VALUES (?, ?)
            ON CONFLICT (week_start) DO NOTHING
            """.trimIndent(),
            Date.valueOf(weekStart),
            Timestamp.from(executedAt)
        ) == 1
}
