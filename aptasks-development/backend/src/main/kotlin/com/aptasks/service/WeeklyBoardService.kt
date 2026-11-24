package com.aptasks.service

import com.aptasks.dto.task.BoardEntryResponse
import com.aptasks.dto.task.BoardWeekResponse
import com.aptasks.dto.task.WeeklyBoardResponse
import com.aptasks.repository.TaskRepository
import com.aptasks.repository.TaskRotationHistoryRepository
import com.aptasks.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Monta o quadro semanal: semana anterior (do histórico), atual (ao vivo) e próxima (prevista). */
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

        val previous = historyRepository.findOutcomesByWeek(weekStart.minusWeeks(1)).map {
            BoardEntryResponse(
                userId = it.userId,
                userName = it.userName,
                taskDescription = it.taskDescription,
                status = when (it.isCompleted) {
                    true -> "done"
                    false -> "missed"
                    null -> "unknown"
                }
            )
        }

        val currentOwners = tasks.mapNotNull { task ->
            val owner = runCatching { UUID.fromString(task.userId) }.getOrNull()?.let { residentsById[it] }
            owner?.let { task to it }
        }
        val current = currentOwners.map { (task, owner) ->
            BoardEntryResponse(owner.id, owner.name, task.description, if (task.isCompleted) "done" else "pending")
        }

        val previousOwners = currentOwners.associate { (task, owner) -> task.id to owner.id }
        val next = rotationService.planWeek(nextStart, tasks, residents, previousOwners).map { (task, owner) ->
            BoardEntryResponse(owner.id, owner.name, task.description, "upcoming")
        }

        return WeeklyBoardResponse(
            listOf(
                week(weekStart.minusWeeks(1), "previous", previous),
                week(weekStart, "current", current),
                week(nextStart, "next", next)
            ),
            extendedUntil = extension?.extendedUntil
        )
    }

    private fun week(start: LocalDate, position: String, entries: List<BoardEntryResponse>) =
        BoardWeekResponse(start, start.plusDays(6), position, entries)
}
