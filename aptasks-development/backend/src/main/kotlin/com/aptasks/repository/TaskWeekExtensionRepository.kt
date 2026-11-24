package com.aptasks.repository

import com.aptasks.model.TaskWeekExtension
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Date
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate

@Repository
class TaskWeekExtensionRepository(private val jdbcTemplate: JdbcTemplate) {
    private val rowMapper = RowMapper { rs, _ ->
        TaskWeekExtension(
            weekStart = rs.getDate("week_start").toLocalDate(),
            extendedUntil = rs.getTimestamp("extended_until").toInstant()
        )
    }

    /** Registra ou substitui a prorrogação da semana. */
    fun upsert(weekStart: LocalDate, extendedUntil: Instant, createdAt: Instant) {
        jdbcTemplate.update(
            """
            INSERT INTO task_week_extensions (week_start, extended_until, created_at)
            VALUES (?, ?, ?)
            ON CONFLICT (week_start) DO UPDATE SET extended_until = EXCLUDED.extended_until
            """.trimIndent(),
            Date.valueOf(weekStart),
            Timestamp.from(extendedUntil),
            Timestamp.from(createdAt)
        )
    }

    /** Prorrogação cujo prazo ainda é posterior a [cutoff], a mais distante primeiro. */
    fun findActive(cutoff: Instant): TaskWeekExtension? = jdbcTemplate.query(
        """
        SELECT week_start, extended_until
        FROM task_week_extensions
        WHERE extended_until > ?
        ORDER BY extended_until DESC
        LIMIT 1
        """.trimIndent(),
        rowMapper,
        Timestamp.from(cutoff)
    ).firstOrNull()

    /** Remove as prorrogações das semanas posteriores a [weekStart]. */
    fun deleteWeeksAfter(weekStart: LocalDate) {
        jdbcTemplate.update("DELETE FROM task_week_extensions WHERE week_start > ?", Date.valueOf(weekStart))
    }
}
