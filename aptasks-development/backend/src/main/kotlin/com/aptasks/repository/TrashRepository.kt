package com.aptasks.repository

import com.aptasks.model.TrashStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/** Persistência da lixeira com SQL declarado explicitamente. */
@Repository
class TrashRepository(private val jdbcTemplate: JdbcTemplate) {
    private val statusRowMapper = RowMapper { resultSet, _ ->
        TrashStatus(
            responsibleUserId = resultSet.getObject("responsible_user_id", UUID::class.java),
            fillLevel = resultSet.getString("fill_level"),
            reportedAt = resultSet.getTimestamp("reported_at")?.toInstant(),
            reportedByUserId = resultSet.getObject("reported_by_user_id", UUID::class.java)
        )
    }

    /** Somente leitura: sem a linha, devolve o estado vazio sem gravar nada. */
    fun findStatus(): TrashStatus = jdbcTemplate.query(
        """
        SELECT responsible_user_id, fill_level, reported_at, reported_by_user_id
        FROM trash_status
        WHERE id = 1
        """.trimIndent(),
        statusRowMapper
    ).firstOrNull() ?: TrashStatus.EMPTY

    fun ensureRow() {
        jdbcTemplate.update(
            """
            INSERT INTO trash_status (id, fill_level)
            VALUES (1, 'EMPTY')
            ON CONFLICT (id) DO NOTHING
            """.trimIndent()
        )
    }

    fun setResponsible(userId: UUID, now: Instant) {
        jdbcTemplate.update(
            """
            UPDATE trash_status
            SET responsible_user_id = ?, updated_at = ?
            WHERE id = 1
            """.trimIndent(),
            userId,
            Timestamp.from(now)
        )
    }

    /** Marca como meio cheio só se estiver vazio. Retorna false quando já não estava vazio. */
    fun markHalfIfEmpty(userId: UUID, now: Instant): Boolean =
        jdbcTemplate.update(
            """
            UPDATE trash_status
            SET fill_level = 'HALF', reported_at = ?, reported_by_user_id = ?, updated_at = ?
            WHERE id = 1 AND fill_level = 'EMPTY'
            """.trimIndent(),
            Timestamp.from(now),
            userId,
            Timestamp.from(now)
        ) == 1

    /** Marca como cheio se estiver vazio ou meio cheio. Retorna false quando já estava cheio. */
    fun markFullIfNotFull(userId: UUID, now: Instant): Boolean =
        jdbcTemplate.update(
            """
            UPDATE trash_status
            SET fill_level = 'FULL', reported_at = ?, reported_by_user_id = ?, updated_at = ?
            WHERE id = 1 AND fill_level IN ('EMPTY', 'HALF')
            """.trimIndent(),
            Timestamp.from(now),
            userId,
            Timestamp.from(now)
        ) == 1

    /** Esvazia só se estiver meio cheio ou cheio. Retorna false quando já estava vazio. */
    fun markEmptiedIfNotEmpty(now: Instant): Boolean =
        jdbcTemplate.update(
            """
            UPDATE trash_status
            SET fill_level = 'EMPTY', reported_at = NULL, reported_by_user_id = NULL, updated_at = ?
            WHERE id = 1 AND fill_level IN ('HALF', 'FULL')
            """.trimIndent(),
            Timestamp.from(now)
        ) == 1

    fun insertEvent(eventType: String, residentId: UUID, now: Instant) {
        jdbcTemplate.update(
            """
            INSERT INTO trash_events (id, event_type, resident_id, occurred_at)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
            UUID.randomUUID(),
            eventType,
            residentId,
            Timestamp.from(now)
        )
    }
}
