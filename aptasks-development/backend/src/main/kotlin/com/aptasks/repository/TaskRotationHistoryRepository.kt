package com.aptasks.repository

import com.aptasks.dto.task.TaskRotationHistoryResponse
import com.aptasks.dto.task.TaskRotationOutcome
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Date
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Repository
class TaskRotationHistoryRepository(private val jdbcTemplate: JdbcTemplate) {
    private val rowMapper = RowMapper { rs, _ ->
        TaskRotationHistoryResponse(
            taskId = rs.getObject("task_id", UUID::class.java),
            taskDescription = rs.getString("task_description"),
            userId = rs.getObject("user_id", UUID::class.java),
            userName = rs.getString("user_name"),
            weekStart = rs.getDate("week_start").toLocalDate(),
            assignedAt = rs.getTimestamp("assigned_at").toInstant()
        )
    }

    fun recordAssignment(
        taskId: UUID,
        taskDescription: String,
        userId: UUID,
        userName: String,
        weekStart: LocalDate,
        assignedAt: Instant
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO task_rotation_history
                (id, task_id, user_id, task_description, user_name, week_start, assigned_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (task_id, week_start) DO UPDATE SET
                user_id = EXCLUDED.user_id,
                task_description = EXCLUDED.task_description,
                user_name = EXCLUDED.user_name,
                assigned_at = EXCLUDED.assigned_at
            """.trimIndent(),
            UUID.randomUUID(),
            taskId,
            userId,
            taskDescription,
            userName,
            Date.valueOf(weekStart),
            Timestamp.from(assignedAt)
        )
    }

    fun findAll(): List<TaskRotationHistoryResponse> = jdbcTemplate.query(
        """
        SELECT task_id, task_description, user_id, user_name, week_start, assigned_at
        FROM task_rotation_history
        ORDER BY week_start DESC, user_name, task_description
        """.trimIndent(),
        rowMapper
    )

    fun findByWeek(weekStart: LocalDate): List<TaskRotationHistoryResponse> = jdbcTemplate.query(
        """
        SELECT task_id, task_description, user_id, user_name, week_start, assigned_at
        FROM task_rotation_history
        WHERE week_start = ?
        """.trimIndent(),
        rowMapper,
        Date.valueOf(weekStart)
    )

    fun findByUserId(userId: UUID): List<TaskRotationHistoryResponse> = jdbcTemplate.query(
        """
        SELECT task_id, task_description, user_id, user_name, week_start, assigned_at
        FROM task_rotation_history
        WHERE user_id = ?
        ORDER BY week_start DESC, task_description
        """.trimIndent(),
        rowMapper,
        userId
    )

    /** Tarefas e resultados de uma semana, para o quadro semanal. */
    fun findOutcomesByWeek(weekStart: LocalDate): List<TaskRotationOutcome> = jdbcTemplate.query(
        """
        SELECT task_id, task_description, user_id, user_name, is_completed, decision_at, fine_applied
        FROM task_rotation_history
        WHERE week_start = ?
        ORDER BY user_name, task_description
        """.trimIndent(),
        RowMapper { rs, _ ->
            val completed = rs.getBoolean("is_completed")
            val completedOrNull = if (rs.wasNull()) null else completed
            val decisionAt = rs.getTimestamp("decision_at")?.toInstant()
            val fined = rs.getBoolean("fine_applied")
            val finedOrNull = if (rs.wasNull()) null else fined
            TaskRotationOutcome(
                taskId = rs.getObject("task_id", UUID::class.java),
                taskDescription = rs.getString("task_description"),
                userId = rs.getObject("user_id", UUID::class.java),
                userName = rs.getString("user_name"),
                isCompleted = completedOrNull,
                decisionAt = decisionAt,
                fineApplied = finedOrNull
            )
        },
        Date.valueOf(weekStart)
    )

    /**
     * Grava o resultado na última semana anterior a [beforeWeek] em que a tarefa foi atribuída,
     * se ela ainda não tiver resultado. Deve rodar antes de a rotação zerar o estado da tarefa.
     */
    fun sealOutcome(taskId: UUID, beforeWeek: LocalDate, isCompleted: Boolean, decisionAt: Instant?, fineApplied: Boolean) {
        jdbcTemplate.update(
            """
            UPDATE task_rotation_history
            SET is_completed = ?, decision_at = ?, fine_applied = ?
            WHERE id = (
                SELECT id FROM task_rotation_history
                WHERE task_id = ? AND week_start < ?
                ORDER BY week_start DESC
                LIMIT 1
            ) AND is_completed IS NULL
            """.trimIndent(),
            isCompleted,
            decisionAt?.let { Timestamp.from(it) },
            fineApplied,
            taskId,
            Date.valueOf(beforeWeek)
        )
    }

    /** Apaga o resultado da semana para que a próxima rotação o grave de novo. */
    fun clearOutcomes(weekStart: LocalDate) {
        jdbcTemplate.update(
            "UPDATE task_rotation_history SET is_completed = NULL, decision_at = NULL, fine_applied = NULL WHERE week_start = ?",
            Date.valueOf(weekStart)
        )
    }

    /** Remove o histórico das semanas posteriores a [weekStart]. */
    fun deleteWeeksAfter(weekStart: LocalDate) {
        jdbcTemplate.update("DELETE FROM task_rotation_history WHERE week_start > ?", Date.valueOf(weekStart))
    }
}
