package com.aptasks.service

import com.aptasks.dto.CreateTaskRequest
import com.aptasks.dto.UpdateTaskRequest
import com.aptasks.exception.TaskNotFoundException
import com.aptasks.model.Task
import com.aptasks.model.User
import com.aptasks.repository.TaskRepository
import com.aptasks.repository.UserRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Operações de CRUD e consultas de TaskService: criação, leitura, edição,
 * exclusão, visão semanal e dashboard.
 */
class TaskServiceCrudTest {
    private val tasks = mockk<TaskRepository>()
    private val users = mockk<UserRepository>()
    private val rotation = mockk<TaskRotationService>()
    private val fines = mockk<FineService>()
    private val service = TaskService(tasks, users, rotation, fines, 5.0, UUID.randomUUID().toString())
    private val ana = User(UUID.randomUUID(), "Ana", "ana@example.test", isAtHome = true)
    private val bia = User(UUID.randomUUID(), "Bia", "bia@example.test", isAtHome = true)

    // ---------- createTask ----------

    @Test
    fun `createTask trims the description, defaults the deadline to seven days and assigns the week`() {
        every { users.findById(ana.id) } returns ana
        val inserted = slot<Task>()
        every { tasks.insert(capture(inserted)) } answers { inserted.captured }
        every { tasks.findById(any()) } answers { inserted.captured }
        every { rotation.assignNewTaskForCurrentWeek(any(), any()) } just runs

        val before = Instant.now()
        val created = service.createTask(
            CreateTaskRequest(responsible_user = ana.id, description = "  Limpar a pia  ")
        )

        assertEquals("Limpar a pia", created.description)
        assertEquals(ana.id.toString(), created.userId)
        assertTrue(created.inTime)
        assertFalse(created.isCompleted)
        val expectedDue = before.plus(Duration.ofDays(7))
        val due = created.dueAt!!
        assertTrue(due.isAfter(expectedDue.minusSeconds(5)) && due.isBefore(expectedDue.plusSeconds(5)))
        verify(exactly = 1) { rotation.assignNewTaskForCurrentWeek(created.id, any()) }
    }

    @Test
    fun `createTask keeps an explicit future deadline`() {
        val deadline = Instant.now().plus(Duration.ofDays(2))
        every { users.findById(ana.id) } returns ana
        val inserted = slot<Task>()
        every { tasks.insert(capture(inserted)) } answers { inserted.captured }
        every { tasks.findById(any()) } answers { inserted.captured }
        every { rotation.assignNewTaskForCurrentWeek(any(), any()) } just runs

        val created = service.createTask(
            CreateTaskRequest(responsible_user = ana.id, description = "Tirar lixo", dueAt = deadline)
        )

        assertEquals(deadline, created.dueAt)
    }

    @Test
    fun `createTask rejects a blank description`() {
        val error = assertThrows<IllegalArgumentException> {
            service.createTask(CreateTaskRequest(responsible_user = ana.id, description = "   "))
        }

        assertEquals("A descrição da tarefa é obrigatória.", error.message)
        verify(exactly = 0) { tasks.insert(any()) }
    }

    @Test
    fun `createTask rejects an unknown responsible resident`() {
        every { users.findById(ana.id) } returns null

        val error = assertThrows<IllegalArgumentException> {
            service.createTask(CreateTaskRequest(responsible_user = ana.id, description = "Lavar louça"))
        }

        assertEquals("O morador responsável não foi encontrado.", error.message)
        verify(exactly = 0) { tasks.insert(any()) }
    }

    @Test
    fun `createTask rejects a deadline in the past`() {
        every { users.findById(ana.id) } returns ana

        val error = assertThrows<IllegalArgumentException> {
            service.createTask(
                CreateTaskRequest(
                    responsible_user = ana.id,
                    description = "Lavar louça",
                    dueAt = Instant.now().minus(Duration.ofDays(1))
                )
            )
        }

        assertEquals("O prazo da tarefa precisa estar no futuro.", error.message)
        verify(exactly = 0) { tasks.insert(any()) }
    }

    @Test
    fun `createTask fails when the inserted task cannot be read back`() {
        every { users.findById(ana.id) } returns ana
        every { tasks.insert(any()) } answers { firstArg() }
        every { tasks.findById(any()) } returns null
        every { rotation.assignNewTaskForCurrentWeek(any(), any()) } just runs

        assertThrows<IllegalStateException> {
            service.createTask(CreateTaskRequest(responsible_user = ana.id, description = "Lavar louça"))
        }
    }

    // ---------- leitura ----------

    @Test
    fun `getTask throws TaskNotFoundException for an unknown id`() {
        val id = UUID.randomUUID()
        every { tasks.findById(id) } returns null

        val error = assertThrows<TaskNotFoundException> { service.getTask(id) }

        assertEquals("Tarefa não encontrada com o ID: $id", error.message)
    }

    @Test
    fun `getWeeklyTasks maps status, fine information and week start`() {
        val now = Instant.parse("2026-10-14T12:00:00Z") // quarta-feira em São Paulo
        val done = task(ana, isCompleted = true, decisionAt = now, fineAppliedAt = now)
        val pending = task(bia)
        every { users.findAll() } returns listOf(ana, bia)
        every { tasks.findAll() } returns listOf(done, pending)

        val result = service.getWeeklyTasks(now)

        assertEquals(2, result.size)
        assertEquals(LocalDate.of(2026, 10, 12), result[0].weekStart)
        assertEquals("done", result[0].status)
        assertTrue(result[0].decisionRecorded)
        assertTrue(result[0].fineApplied)
        assertEquals(5.0, result[0].fineAmount)
        assertEquals(ana.name, result[0].userName)
        assertEquals("pending", result[1].status)
        assertFalse(result[1].decisionRecorded)
        assertFalse(result[1].fineApplied)
        assertEquals(null, result[1].fineAmount)
    }

    @Test
    fun `getWeeklyTasks uses the Sao Paulo calendar for the week boundary`() {
        every { users.findAll() } returns listOf(ana)
        every { tasks.findAll() } returns listOf(task(ana))

        // Domingo 22h em São Paulo (ainda da semana anterior, UTC já é segunda)
        val sundayNight = service.getWeeklyTasks(Instant.parse("2026-10-19T01:00:00Z"))
        // Segunda 01h em São Paulo: já é a nova semana
        val mondayEarly = service.getWeeklyTasks(Instant.parse("2026-10-19T04:00:00Z"))

        assertEquals(LocalDate.of(2026, 10, 12), sundayNight[0].weekStart)
        assertEquals(LocalDate.of(2026, 10, 19), mondayEarly[0].weekStart)
    }

    @Test
    fun `getWeeklyTasks fails when a task points to a resident that does not exist`() {
        every { users.findAll() } returns listOf(ana)
        every { tasks.findAll() } returns listOf(task(bia))

        assertThrows<IllegalStateException> { service.getWeeklyTasks(Instant.now()) }
    }

    @Test
    fun `getDashboardData counts totals and returns at most five recent tasks`() {
        val all = (1..6).map { index -> task(ana, isCompleted = index % 3 == 0) }
        every { tasks.findAll() } returns all

        val dashboard = service.getDashboardData()

        assertEquals(6, dashboard.totalTasks)
        assertEquals(2, dashboard.completedTasks)
        assertEquals(4, dashboard.pendingTasks)
        assertEquals(all.take(5), dashboard.recentTasks)
    }

    @Test
    fun `getDashboardData returns zeros when there are no tasks`() {
        every { tasks.findAll() } returns emptyList()

        val dashboard = service.getDashboardData()

        assertEquals(0, dashboard.totalTasks)
        assertEquals(0, dashboard.completedTasks)
        assertEquals(0, dashboard.pendingTasks)
        assertTrue(dashboard.recentTasks.isEmpty())
    }

    // ---------- updateTask ----------

    @Test
    fun `updateTask trims the description and changes responsible and deadline`() {
        val existing = task(ana)
        val newDeadline = Instant.now().plus(Duration.ofDays(3))
        every { tasks.findById(existing.id) } returns existing
        every { users.findById(bia.id) } returns bia
        every { tasks.update(any()) } answers { firstArg() }

        val updated = service.updateTask(
            existing.id,
            UpdateTaskRequest(responsible_user = bia.id, description = "  Passar pano  ", dueAt = newDeadline)
        )

        assertEquals("Passar pano", updated.description)
        assertEquals(bia.id.toString(), updated.userId)
        assertEquals(newDeadline, updated.dueAt)
        verify(exactly = 1) { tasks.update(existing) }
    }

    @Test
    fun `updateTask keeps the current deadline when none is sent`() {
        val originalDue = Instant.now().plus(Duration.ofDays(1))
        val existing = task(ana, dueAt = originalDue)
        every { tasks.findById(existing.id) } returns existing
        every { users.findById(ana.id) } returns ana
        every { tasks.update(any()) } answers { firstArg() }

        val updated = service.updateTask(
            existing.id,
            UpdateTaskRequest(responsible_user = ana.id, description = "Mesma tarefa")
        )

        assertEquals(originalDue, updated.dueAt)
    }

    @Test
    fun `updateTask rejects a blank description before reading the task`() {
        assertThrows<IllegalArgumentException> {
            service.updateTask(UUID.randomUUID(), UpdateTaskRequest(responsible_user = ana.id, description = ""))
        }

        verify(exactly = 0) { tasks.findById(any()) }
        verify(exactly = 0) { tasks.update(any()) }
    }

    @Test
    fun `updateTask throws TaskNotFoundException when the task does not exist`() {
        val id = UUID.randomUUID()
        every { tasks.findById(id) } returns null

        assertThrows<TaskNotFoundException> {
            service.updateTask(id, UpdateTaskRequest(responsible_user = ana.id, description = "Algo"))
        }

        verify(exactly = 0) { tasks.update(any()) }
    }

    @Test
    fun `updateTask rejects a deadline in the past without persisting`() {
        val existing = task(ana)
        every { tasks.findById(existing.id) } returns existing
        every { users.findById(ana.id) } returns ana

        assertThrows<IllegalArgumentException> {
            service.updateTask(
                existing.id,
                UpdateTaskRequest(
                    responsible_user = ana.id,
                    description = "Algo",
                    dueAt = Instant.now().minus(Duration.ofHours(1))
                )
            )
        }

        verify(exactly = 0) { tasks.update(any()) }
    }

    // ---------- deleteTask ----------

    @Test
    fun `deleteTask succeeds when the repository removes a row`() {
        val id = UUID.randomUUID()
        every { tasks.deleteById(id) } returns true

        service.deleteTask(id)

        verify(exactly = 1) { tasks.deleteById(id) }
    }

    @Test
    fun `deleteTask throws TaskNotFoundException when nothing was deleted`() {
        val id = UUID.randomUUID()
        every { tasks.deleteById(id) } returns false

        assertThrows<TaskNotFoundException> { service.deleteTask(id) }
    }

    // ---------- helpers ----------

    private fun task(
        resident: User,
        isCompleted: Boolean = false,
        dueAt: Instant? = null,
        decisionAt: Instant? = null,
        fineAppliedAt: Instant? = null
    ) = Task(
        id = UUID.randomUUID(),
        description = "Tarefa de ${resident.name}",
        userId = resident.id.toString(),
        isCompleted = isCompleted,
        dueAt = dueAt,
        decisionAt = decisionAt,
        fineAppliedAt = fineAppliedAt
    )
}
