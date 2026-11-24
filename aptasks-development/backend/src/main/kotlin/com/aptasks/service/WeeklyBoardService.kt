package com.aptasks.service

import com.aptasks.dto.task.BoardEntryResponse
import com.aptasks.dto.task.BoardWeekResponse
import com.aptasks.dto.task.TaskRotationOutcome
import com.aptasks.dto.task.WeeklyBoardResponse
import com.aptasks.repository.TaskRepository
import com.aptasks.repository.TaskRotationHistoryRepository
import com.aptasks.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Monta o quadro semanal com cinco semanas: duas anteriores (do histórico), a atual (ao vivo) e
 * duas próximas (previstas pela mesma regra da rotação, sem gravar nada).
 */
@Service
class WeeklyBoardService(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val historyRepository: TaskRotationHistoryRepository,
    private val rotationService: TaskRotationService
) {
    fun getBoard(now: Instant = Instant.now()): WeeklyBoardResponse {
        // Com o prazo prorrogado, a semana antiga continua sendo a atual até a rotação acontecer.
        val extension = rotationService.activeExtension(now)
        val calendarStart = rotationService.calendarWeekStart(now)
        val weekStart = extension?.weekStart ?: calendarStart
        val nextStart = maxOf(weekStart.plusWeeks(1), calendarStart)
        val residents = userRepository.findAll()
        val residentsById = residents.associateBy { it.id }
        val tasks = taskRepository.findAll()

        val currentOwners = tasks.mapNotNull { task ->
            val owner = runCatching { UUID.fromString(task.userId) }.getOrNull()?.let { residentsById[it] }
            owner?.let { task to it }
        }
        val current = currentOwners.map { (task, owner) ->
            BoardEntryResponse(owner.id, owner.name, task.description, if (task.isCompleted) "done" else "pending")
        }

        // Cada semana prevista usa os responsáveis da anterior para ninguém repetir a tarefa.
        val firstOwners = currentOwners.associate { (task, owner) -> task.id to owner.id }
        val nextPlan = rotationService.planWeek(nextStart, tasks, residents, firstOwners)
        val secondOwners = nextPlan.associate { (task, owner) -> task.id to owner.id }
        val nextPlan2 = rotationService.planWeek(nextStart.plusWeeks(1), tasks, residents, secondOwners)

        return WeeklyBoardResponse(
            listOf(
                week(weekStart.minusWeeks(2), "previous2", past(weekStart.minusWeeks(2))),
                week(weekStart.minusWeeks(1), "previous", past(weekStart.minusWeeks(1))),
                week(weekStart, "current", current),
                week(nextStart, "next", upcoming(nextPlan)),
                week(nextStart.plusWeeks(1), "next2", upcoming(nextPlan2))
            ),
            extendedUntil = extension?.extendedUntil
        )
    }

    private fun past(weekStart: LocalDate): List<BoardEntryResponse> =
        historyRepository.findOutcomesByWeek(weekStart).map(::entryFor)

    private fun entryFor(outcome: TaskRotationOutcome) = BoardEntryResponse(
        userId = outcome.userId,
        userName = outcome.userName,
        taskDescription = outcome.taskDescription,
        status = when (outcome.isCompleted) {
            true -> "done"
            false -> "missed"
            null -> "unknown"
        }
    )

    private fun upcoming(plan: List<Pair<com.aptasks.model.Task, com.aptasks.model.User>>) =
        plan.map { (task, owner) -> BoardEntryResponse(owner.id, owner.name, task.description, "upcoming") }

    private fun week(start: LocalDate, position: String, entries: List<BoardEntryResponse>) =
        BoardWeekResponse(start, start.plusDays(6), position, entries)
}
