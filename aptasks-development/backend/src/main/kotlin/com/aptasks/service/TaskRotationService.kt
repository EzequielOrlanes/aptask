package com.aptasks.service

import com.aptasks.repository.TaskRepository
import com.aptasks.repository.TaskRotationHistoryRepository
import com.aptasks.repository.TaskRotationRepository
import com.aptasks.repository.UserRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.util.UUID

private val rotationLogger = KotlinLogging.logger {}

data class WeeklyRotationResult(
    val weekStart: LocalDate,
    val rotated: Boolean,
    val taskCount: Int,
    val memberCount: Int,
    val assignmentsPerMember: Map<String, Int>
)

@Service
class TaskRotationService(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val taskRotationRepository: TaskRotationRepository,
    private val taskRotationHistoryRepository: TaskRotationHistoryRepository
) {
    private val householdZone = ZoneId.of("America/Sao_Paulo")
    private val rotationAnchor = LocalDate.of(1970, 1, 5) // segunda-feira

    /**
     * Distribui todas as tarefas cadastradas entre todos os moradores.
     * A diferença semanal entre o maior e o menor número de tarefas é no
     * máximo uma; a tarefa extra e os responsáveis avançam a cada semana.
     */
    @Transactional
    fun rotateCurrentWeek(now: Instant = Instant.now()): WeeklyRotationResult {
        val today = now.atZone(householdZone).toLocalDate()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val tasks = taskRepository.findAll()
        val residents = userRepository.findAll()

        if (tasks.isEmpty() || residents.isEmpty()) {
            return WeeklyRotationResult(weekStart, false, tasks.size, residents.size, emptyMap())
        }

        if (!taskRotationRepository.registerWeekIfAbsent(weekStart, now)) {
            return WeeklyRotationResult(weekStart, false, tasks.size, residents.size, emptyMap())
        }

        val weekIndex = ChronoUnit.WEEKS.between(rotationAnchor, weekStart)
        val deadline = weekDeadline(weekStart)
        val counts = residents.associate { it.id to 0 }.toMutableMap()
        val previousOwners = taskRotationHistoryRepository.findByWeek(weekStart.minusWeeks(1))
            .mapNotNull { entry -> entry.taskId?.let { it to entry.userId } }
            .toMap()

        tasks.forEachIndexed { taskIndex, task ->
            var residentIndex = Math.floorMod(taskIndex.toLong() + weekIndex, residents.size.toLong()).toInt()
            // Proteção: se a tarefa voltou para quem a teve na semana anterior, avança para o próximo morador.
            if (residents.size > 1 && previousOwners[task.id] == residents[residentIndex].id) {
                rotationLogger.warn {
                    "Tarefa '${task.description}' repetiu o responsável ${residents[residentIndex].name} " +
                        "na semana $weekStart; avançando para o próximo morador."
                }
                residentIndex = Math.floorMod(residentIndex + 1, residents.size)
            }
            val responsible = residents[residentIndex]
            val updated = taskRepository.assignForWeek(task.id, responsible.id, deadline)
            check(updated) { "A tarefa ${task.id} deixou de existir durante a rotação semanal." }
            taskRotationHistoryRepository.recordAssignment(
                taskId = task.id,
                taskDescription = task.description,
                userId = responsible.id,
                userName = responsible.name,
                weekStart = weekStart,
                assignedAt = now
            )
            counts[responsible.id] = counts.getValue(responsible.id) + 1
        }

        return WeeklyRotationResult(
            weekStart = weekStart,
            rotated = true,
            taskCount = tasks.size,
            memberCount = residents.size,
            assignmentsPerMember = residents.associate { it.name to counts.getValue(it.id) }
        )
    }

    /** Coloca uma nova tarefa na semana atual sem mexer nas tarefas já concluídas. */
    @Transactional
    fun assignNewTaskForCurrentWeek(taskId: UUID, now: Instant = Instant.now()) {
        val today = now.atZone(householdZone).toLocalDate()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        if (!taskRotationRepository.hasRun(weekStart)) {
            rotateCurrentWeek(now)
            return
        }
        val tasks = taskRepository.findAll()
        val residents = userRepository.findAll()
        if (residents.isEmpty()) return
        val newTaskIndex = tasks.indexOfFirst { it.id == taskId }
        if (newTaskIndex < 0) return

        val counts = residents.associate { it.id to 0 }.toMutableMap()
        tasks.filterNot { it.id == taskId }.forEach { task ->
            val residentId = runCatching { UUID.fromString(task.userId) }.getOrNull()
            if (residentId != null && counts.containsKey(residentId)) {
                counts[residentId] = counts.getValue(residentId) + 1
            }
        }

        val lowestCount = counts.values.minOrNull() ?: return
        val leastLoadedResidents = residents.filter { counts.getValue(it.id) == lowestCount }
        val weekIndex = ChronoUnit.WEEKS.between(rotationAnchor, weekStart)
        val chosenIndex = Math.floorMod(weekIndex + newTaskIndex, leastLoadedResidents.size.toLong()).toInt()
        val responsible = leastLoadedResidents[chosenIndex]
        val updated = taskRepository.assignForWeek(taskId, responsible.id, weekDeadline(weekStart))
        check(updated) { "A tarefa $taskId deixou de existir durante a atribuição semanal." }
        val task = tasks.first { it.id == taskId }
        taskRotationHistoryRepository.recordAssignment(
            taskId = task.id,
            taskDescription = task.description,
            userId = responsible.id,
            userName = responsible.name,
            weekStart = weekStart,
            assignedAt = now
        )
    }

    fun getHistory(userId: UUID? = null) =
        if (userId == null) taskRotationHistoryRepository.findAll()
        else taskRotationHistoryRepository.findByUserId(userId)

    private fun weekDeadline(weekStart: LocalDate): Instant =
        weekStart.plusDays(7).atTime(LocalTime.MIDNIGHT).minusSeconds(1)
            .atZone(householdZone).toInstant()
}
