package com.aptasks.repository

import com.aptasks.dto.task.TaskRotationHistoryResponse
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
}
