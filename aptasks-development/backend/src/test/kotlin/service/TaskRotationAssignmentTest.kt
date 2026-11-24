package com.aptasks.service

import com.aptasks.dto.task.TaskRotationHistoryResponse
import com.aptasks.model.Task
import com.aptasks.model.User
import com.aptasks.repository.TaskRepository
import com.aptasks.repository.TaskRotationHistoryRepository
import com.aptasks.repository.TaskRotationRepository
import com.aptasks.repository.TaskWeekExtensionRepository
import com.aptasks.model.TaskWeekExtension
import com.aptasks.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Atribuição de tarefas novas dentro da semana corrente, histórico e
 * comportamentos de borda da rotação semanal.
 */
class TaskRotationAssignmentTest {
    private val tasks = mockk<TaskRepository>()
    private val users = mockk<UserRepository>()
    private val rotationRuns = mockk<TaskRotationRepository>()
    private val history = mockk<TaskRotationHistoryRepository>(relaxed = true)
    private val extensions = mockk<TaskWeekExtensionRepository>()
    private val service = TaskRotationService(tasks, users, rotationRuns, history, extensions)
    private val now = Instant.parse("2026-10-14T12:00:00Z") // semana de 12/10/2026
    private val weekStart = LocalDate.of(2026, 10, 12)
    private val ana = resident("Ana")
    private val bia = resident("Bia")

    init {
        every { history.findByWeek(any()) } returns emptyList()
        every { extensions.findActive(any()) } returns null
    }

    @Test
    fun `new task created during an extension joins the extended week with the extended deadline`() {
        val extension = TaskWeekExtension(LocalDate.of(2026, 10, 5), now.plusSeconds(7200))
        val anaTask = task(ana.id.toString())
        val newTask = task("")
        every { extensions.findActive(any()) } returns extension
        every { rotationRuns.hasRun(extension.weekStart) } returns true
        every { tasks.findAll() } returns listOf(anaTask, newTask)
        every { users.findAll() } returns listOf(ana, bia)
        every { tasks.assignForWeek(any(), any(), any()) } returns true

        service.assignNewTaskForCurrentWeek(newTask.id, now)

        verify(exactly = 1) { tasks.assignForWeek(newTask.id, bia.id, extension.extendedUntil) }
    }

    @Test
    fun `new task goes to the least loaded resident when the week was already rotated`() {
        val anaTask1 = task(ana.id.toString())
        val anaTask2 = task(ana.id.toString())
        val newTask = task("")
        every { rotationRuns.hasRun(weekStart) } returns true
        every { tasks.findAll() } returns listOf(anaTask1, anaTask2, newTask)
        every { users.findAll() } returns listOf(ana, bia)
        every { tasks.assignForWeek(newTask.id, bia.id, any()) } returns true

        service.assignNewTaskForCurrentWeek(newTask.id, now)

        verify(exactly = 1) { tasks.assignForWeek(newTask.id, bia.id, any()) }
        verify(exactly = 1) {
            history.recordAssignment(
                taskId = newTask.id,
                taskDescription = newTask.description,
                userId = bia.id,
                userName = bia.name,
                weekStart = weekStart,
                assignedAt = now
            )
        }
    }

    @Test
    fun `new task triggers a full rotation when the week has never been rotated`() {
        val existing = task(ana.id.toString())
        val newTask = task("")
        every { rotationRuns.hasRun(weekStart) } returns false
        every { rotationRuns.registerWeekIfAbsent(any(), now) } returns true
        every { tasks.findAll() } returns listOf(existing, newTask)
        every { users.findAll() } returns listOf(ana)
        every { tasks.assignForWeek(any(), any(), any()) } returns true

        service.assignNewTaskForCurrentWeek(newTask.id, now)

        verify(exactly = 1) { rotationRuns.registerWeekIfAbsent(weekStart, now) }
        verify(exactly = 2) { tasks.assignForWeek(any(), ana.id, any()) }
    }

    @Test
    fun `new task is not assigned when there are no residents`() {
        val newTask = task("")
        every { rotationRuns.hasRun(weekStart) } returns true
        every { tasks.findAll() } returns listOf(newTask)
        every { users.findAll() } returns emptyList()

        service.assignNewTaskForCurrentWeek(newTask.id, now)

        verify(exactly = 0) { tasks.assignForWeek(any(), any(), any()) }
        verify(exactly = 0) { history.recordAssignment(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `new task is ignored when it is not part of the household task list`() {
        val unknownId = UUID.randomUUID()
        every { rotationRuns.hasRun(weekStart) } returns true
        every { tasks.findAll() } returns listOf(task(ana.id.toString()))
        every { users.findAll() } returns listOf(ana, bia)

        service.assignNewTaskForCurrentWeek(unknownId, now)

        verify(exactly = 0) { tasks.assignForWeek(any(), any(), any()) }
    }

    @Test
    fun `rotation fails when a task disappears before being assigned`() {
        every { rotationRuns.registerWeekIfAbsent(any(), any()) } returns true
        every { tasks.findAll() } returns listOf(task(""))
        every { users.findAll() } returns listOf(ana)
        every { tasks.assignForWeek(any(), any(), any()) } returns false

        assertThrows<IllegalStateException> { service.rotateCurrentWeek(now) }
    }

    @Test
    fun `the first resident changes from one week to the next`() {
        val first = task("")
        val assignments = mutableMapOf<UUID, UUID>()
        every { rotationRuns.registerWeekIfAbsent(any(), any()) } returns true
        every { tasks.findAll() } returns listOf(first, task(""))
        every { users.findAll() } returns listOf(ana, bia)
        every { tasks.assignForWeek(any(), any(), any()) } answers {
            assignments[firstArg<UUID>()] = secondArg<UUID>()
            true
        }

        service.rotateCurrentWeek(now)
        val weekOneResident = assignments.getValue(first.id)
        service.rotateCurrentWeek(now.plus(Duration.ofDays(7)))
        val weekTwoResident = assignments.getValue(first.id)

        assertNotEquals(weekOneResident, weekTwoResident)
    }

    @Test
    fun `getHistory without a user returns the whole history`() {
        every { history.findAll() } returns listOf(historyEntry())

        val result = service.getHistory()

        assertEquals(1, result.size)
        verify(exactly = 1) { history.findAll() }
        verify(exactly = 0) { history.findByUserId(any()) }
    }

    @Test
    fun `getHistory with a user filters by that user`() {
        every { history.findByUserId(ana.id) } returns listOf(historyEntry())

        service.getHistory(ana.id)

        verify(exactly = 1) { history.findByUserId(ana.id) }
        verify(exactly = 0) { history.findAll() }
    }

    private fun resident(name: String) = User(UUID.randomUUID(), name, "$name@example.test", true)

    private fun task(userId: String) = Task(
        id = UUID.randomUUID(),
        description = "Tarefa",
        userId = userId
    )

    private fun historyEntry() = TaskRotationHistoryResponse(
        taskId = UUID.randomUUID(),
        taskDescription = "Tarefa",
        userId = ana.id,
        userName = ana.name,
        weekStart = weekStart,
        assignedAt = now
    )
}
