package com.aptasks.service

import com.aptasks.dto.CreateTaskRequest
import com.aptasks.model.Task
import com.aptasks.repository.TaskRepository
import com.aptasks.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID
import com.aptasks.dto.DashboardResponse
import com.aptasks.dto.UpdateTaskRequest
import com.aptasks.dto.fine.FineRequest
import com.aptasks.dto.task.WeeklyTaskResponse
import com.aptasks.exception.TaskNotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters


@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val taskRotationService: TaskRotationService,
    private val fineService: FineService,
    @Value("\${tasks.fine.amount:5.00}") private val fineAmount: Double,
    @Value("\${tasks.fine.sender-id:}") private val fineSenderId: String
){
    @Transactional
    fun createTask(request: CreateTaskRequest): Task {
        require(request.description.isNotBlank()) { "A descrição da tarefa é obrigatória." }
        val responsible = userRepository.findById(request.responsible_user)
            ?: throw IllegalArgumentException("O morador responsável não foi encontrado.")
        val dueAt = request.dueAt ?: Instant.now().plus(Duration.ofDays(7))
        require(dueAt.isAfter(Instant.now())) { "O prazo da tarefa precisa estar no futuro." }
        val newTask = Task(
            id = UUID.randomUUID(),
            description = request.description.trim(),
            inTime = true,
            userId = responsible.id.toString(),
            isCompleted = false,
            dueAt = dueAt
        )
        taskRepository.insert(newTask)
        taskRotationService.assignNewTaskForCurrentWeek(newTask.id)
        return taskRepository.findById(newTask.id)
            ?: throw IllegalStateException("A tarefa recém-criada não foi encontrada.")
    }

    fun getAllTasks(): List<Task> = taskRepository.findAll()

    fun getWeeklyTasks(now: Instant = Instant.now()): List<WeeklyTaskResponse> {
        val residentsById = userRepository.findAll().associateBy { it.id }
        val weekStart = now.atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        return taskRepository.findAll().map { task ->
            val residentId = UUID.fromString(task.userId)
            val resident = residentsById[residentId]
                ?: throw IllegalStateException("O responsável da tarefa ${task.id} não foi encontrado.")
            WeeklyTaskResponse(
                id = task.id,
                description = task.description,
                userId = resident.id,
                userName = resident.name,
                weekStart = weekStart,
                deadline = task.dueAt,
                status = if (task.isCompleted) "done" else "pending",
                decisionRecorded = task.decisionAt != null,
                fineApplied = task.fineAppliedAt != null,
                fineAmount = if (task.fineAppliedAt != null) fineAmount else null
            )
        }
    }

    fun getTask(id: UUID): Task = taskRepository.findById(id)
        ?: throw TaskNotFoundException(id)

    @Transactional
    fun updateTask(id: UUID, request: UpdateTaskRequest): Task {
        require(request.description.isNotBlank()) { "A descrição da tarefa é obrigatória." }
        val task = getTask(id)
        val newResponsible = userRepository.findById(request.responsible_user)
            ?: throw IllegalArgumentException("O morador responsável não foi encontrado.")
        request.dueAt?.let { require(it.isAfter(Instant.now())) { "O prazo da tarefa precisa estar no futuro." } }
        task.description = request.description.trim()
        task.userId = newResponsible.id.toString()
        request.dueAt?.let { task.dueAt = it }
        return taskRepository.update(task)
    }

    @Transactional
    fun deleteTask(id: UUID) {
        val deleted = taskRepository.deleteById(id)
        if (!deleted) {
            throw TaskNotFoundException(id)
        }
    }

    /**
     * Avalia tarefas vencidas. Se o responsável estiver fora, concede 24 horas
     * adicionais; para quem está em casa, registra a multa e marca a tarefa
     * como processada dentro da mesma transação.
     */
    @Transactional
    fun evaluateOverdueTasks(now: Instant = Instant.now()): Int {
        val overdueTasks = taskRepository.findOverdue(now)
        var postponed = 0

        overdueTasks.forEach { task ->
            val responsibleId = runCatching { UUID.fromString(task.userId) }.getOrNull()
                ?: throw IllegalStateException("A tarefa ${task.id} tem um responsável com ID inválido.")
            val responsible = userRepository.findById(responsibleId)
                ?: throw IllegalStateException("O responsável da tarefa ${task.id} não foi encontrado.")

            if (!responsible.isAtHome) {
                taskRepository.postponeDeadline(task.id, now.plus(Duration.ofHours(24)))
                postponed++
            } else {
                applyTaskFine(task, responsibleId, now)
            }
        }
        return postponed
    }


    fun getDashboardData(): DashboardResponse {
        val allTasks = taskRepository.findAll()
        val total = allTasks.size
        var completed = allTasks.count { it.isCompleted }
        var pending = total - completed
        val recent = allTasks.take(5)

        return DashboardResponse(total, completed, pending, recent)
    }

    @Transactional
    fun toggleStatus(id: UUID) {
        val task = getTask(id)
        setCompletionInternal(task, !task.isCompleted)
    }

    @Transactional
    fun setCompletion(id: UUID, done: Boolean): WeeklyTaskResponse {
        val task = getTask(id)
        setCompletionInternal(task, done)
        return getWeeklyTasks().first { it.id == id }
    }

    private fun setCompletionInternal(task: Task, done: Boolean) {
        task.isCompleted = done
        task.decisionAt = Instant.now()
        taskRepository.update(task)
        val now = Instant.now()
        if (task.dueAt?.isBefore(now) == true && task.fineAppliedAt == null) {
            val responsibleId = UUID.fromString(task.userId)
            val responsible = userRepository.findById(responsibleId)
                ?: throw IllegalStateException("O responsável da tarefa ${task.id} não foi encontrado.")

            // Decisão ou conclusão tardia é avaliada imediatamente sem esperar o scheduler.
            if (responsible.isAtHome) {
                applyTaskFine(task, responsibleId, now)
            }
        }
    }

    private fun applyTaskFine(task: Task, responsibleId: UUID, at: Instant) {
        require(fineAmount > 0.0) { "O valor configurado para a multa precisa ser maior que zero." }
        val senderId = runCatching { UUID.fromString(fineSenderId) }.getOrNull()
            ?: throw IllegalStateException(
                "Configure TASK_FINE_SENDER_ID com o UUID de um morador ou da conta da casa para registrar multas."
            )

        fineService.applyFine(
            FineRequest(
                amount = fineAmount,
                sender = senderId.toString(),
                receiver = responsibleId.toString(),
                timestamp = LocalDateTime.ofInstant(at, ZoneId.systemDefault())
            )
        )
        taskRepository.markFineApplied(task.id, at)
    }
}
