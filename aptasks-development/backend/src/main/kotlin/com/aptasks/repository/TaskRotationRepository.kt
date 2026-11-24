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

    /** Semana mais recente já rotacionada, ou null se nenhuma rotação aconteceu. */
    fun latestWeek(): LocalDate? =
        jdbcTemplate.queryForObject("SELECT MAX(week_start) FROM task_rotation_runs", Date::class.java)?.toLocalDate()

    /** Instante em que a semana foi rotacionada, ou null se ela não foi. */
    fun executedAt(weekStart: LocalDate): Instant? = jdbcTemplate.query(
        "SELECT executed_at FROM task_rotation_runs WHERE week_start = ?",
        { rs, _ -> rs.getTimestamp("executed_at").toInstant() },
        Date.valueOf(weekStart)
    ).firstOrNull()

    /** Esquece as rotações posteriores a [weekStart], para que aconteçam de novo. */
    fun deleteWeeksAfter(weekStart: LocalDate) {
        jdbcTemplate.update("DELETE FROM task_rotation_runs WHERE week_start > ?", Date.valueOf(weekStart))
    }
}
