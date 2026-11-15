package com.aptasks.repository

import com.aptasks.exception.TaskNotFoundException
import com.aptasks.model.Task
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID
import java.sql.Timestamp
import java.time.Instant

/**
 * Acesso a dados de tarefas. As instruções SQL ficam explícitas aqui para
 * facilitar o estudo e a manutenção das consultas.
 */
@Repository
class TaskRepository(private val jdbcTemplate: JdbcTemplate) {

    private val taskRowMapper = RowMapper { resultSet, _ ->
        Task(
            id = UUID.fromString(resultSet.getString("id")),
            description = resultSet.getString("description"),
            inTime = resultSet.getBoolean("in_time"),
            userId = resultSet.getString("user_id"),
            isCompleted = resultSet.getBoolean("is_completed"),
            dueAt = resultSet.getTimestamp("due_at")?.toInstant(),
            fineAppliedAt = resultSet.getTimestamp("fine_applied_at")?.toInstant(),
            decisionAt = resultSet.getTimestamp("decision_at")?.toInstant()
        )
    }

    fun findAll(): List<Task> = jdbcTemplate.query(
        """
        SELECT id, description, in_time, user_id, is_completed, due_at, fine_applied_at, decision_at
        FROM tasks
        ORDER BY id
        """.trimIndent(),
        taskRowMapper
    )

    fun findById(id: UUID): Task? = jdbcTemplate.query(
        """
        SELECT id, description, in_time, user_id, is_completed, due_at, fine_applied_at, decision_at
        FROM tasks
        WHERE id = ?
        """.trimIndent(),
        taskRowMapper,
        id
    ).firstOrNull()

    /** Retorna e bloqueia tarefas vencidas para evitar avaliação simultânea por schedulers. */
    fun findOverdue(now: Instant): List<Task> = jdbcTemplate.query(
        """
        SELECT id, description, in_time, user_id, is_completed, due_at, fine_applied_at, decision_at
        FROM tasks
        WHERE is_completed = false
          AND due_at IS NOT NULL
          AND due_at <= ?
          AND fine_applied_at IS NULL
        ORDER BY due_at
        FOR UPDATE SKIP LOCKED
        """.trimIndent(),
        taskRowMapper,
        Timestamp.from(now)
    )

    fun insert(task: Task): Task {
        jdbcTemplate.update(
            """
            INSERT INTO tasks (id, description, in_time, user_id, is_completed, due_at, fine_applied_at, decision_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            task.id,
            task.description,
            task.inTime,
            task.userId,
            task.isCompleted,
            task.dueAt?.let(Timestamp::from),
            task.fineAppliedAt?.let(Timestamp::from),
            task.decisionAt?.let(Timestamp::from)
        )
        return task
    }

    fun update(task: Task): Task {
        val updatedRows = jdbcTemplate.update(
            """
            UPDATE tasks
            SET description = ?, in_time = ?, user_id = ?, is_completed = ?, due_at = ?, fine_applied_at = ?, decision_at = ?
            WHERE id = ?
            """.trimIndent(),
            task.description,
            task.inTime,
            task.userId,
            task.isCompleted,
            task.dueAt?.let(Timestamp::from),
            task.fineAppliedAt?.let(Timestamp::from),
            task.decisionAt?.let(Timestamp::from),
            task.id
        )

        if (updatedRows == 0) {
            throw TaskNotFoundException(task.id)
        }

        return task
    }

    /** Redistribui a tarefa existente e inicia o estado da nova semana. */
    fun assignForWeek(taskId: UUID, residentId: UUID, dueAt: Instant): Boolean =
        jdbcTemplate.update(
            """
            UPDATE tasks
            SET user_id = ?, due_at = ?, is_completed = false, fine_applied_at = NULL, decision_at = NULL
            WHERE id = ?
            """.trimIndent(),
            residentId.toString(),
            Timestamp.from(dueAt),
            taskId
        ) > 0

    fun postponeDeadline(taskId: UUID, newDeadline: Instant) {
        jdbcTemplate.update(
            "UPDATE tasks SET due_at = ? WHERE id = ?",
            Timestamp.from(newDeadline),
            taskId
        )
    }

    fun markFineApplied(taskId: UUID, appliedAt: Instant) {
        jdbcTemplate.update(
            "UPDATE tasks SET fine_applied_at = ? WHERE id = ? AND fine_applied_at IS NULL",
            Timestamp.from(appliedAt),
            taskId
        )
    }

    fun deleteById(id: UUID): Boolean {
        val deletedRows = jdbcTemplate.update(
            "DELETE FROM tasks WHERE id = ?",
            id
        )
        return deletedRows > 0
    }
}
