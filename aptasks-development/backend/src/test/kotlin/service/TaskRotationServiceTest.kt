package com.aptasks.service

import com.aptasks.dto.task.TaskRotationHistoryResponse
import com.aptasks.model.Task
import com.aptasks.model.User
import com.aptasks.repository.TaskRepository
import com.aptasks.repository.TaskRotationHistoryRepository
import com.aptasks.repository.TaskRotationRepository
import com.aptasks.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

class TaskRotationServiceTest {
    private val tasks = mockk<TaskRepository>()
    private val users = mockk<UserRepository>()
    private val rotationRuns = mockk<TaskRotationRepository>()
    private val history = mockk<TaskRotationHistoryRepository>(relaxed = true)
    private val service = TaskRotationService(tasks, users, rotationRuns, history)
    private val now = Instant.parse("2026-10-14T12:00:00Z")

    init {
        every { history.findByWeek(any()) } returns emptyList()
    }

    @Test
    fun `moves a task to the next resident when it would repeat last week's owner`() {
        val residents = listOf(resident("Ana"), resident("Bia"))
        val task = tasks(1).single()
        val weekStart = LocalDate.parse("2026-10-12")
        val naturalOwner = residents[Math.floorMod(ChronoUnit.WEEKS.between(ANCHOR, weekStart), 2L).toInt()]
        val expectedOwner = residents.first { it.id != naturalOwner.id }
        stubRotation(residents, listOf(task))
        every { history.findByWeek(weekStart.minusWeeks(1)) } returns listOf(
            historyEntry(task.id, naturalOwner, weekStart.minusWeeks(1))
        )

        service.rotateCurrentWeek(now)

        verify(exactly = 1) { tasks.assignForWeek(task.id, expectedOwner.id, any()) }
        verify(exactly = 0) { tasks.assignForWeek(task.id, naturalOwner.id, any()) }
    }

    @Test
    fun `keeps the natural owner when last week's owner was someone else`() {
        val residents = listOf(resident("Ana"), resident("Bia"))
        val task = tasks(1).single()
        val weekStart = LocalDate.parse("2026-10-12")
        val naturalOwner = residents[Math.floorMod(ChronoUnit.WEEKS.between(ANCHOR, weekStart), 2L).toInt()]
        val otherOwner = residents.first { it.id != naturalOwner.id }
        stubRotation(residents, listOf(task))
        every { history.findByWeek(weekStart.minusWeeks(1)) } returns listOf(
            historyEntry(task.id, otherOwner, weekStart.minusWeeks(1))
        )

        service.rotateCurrentWeek(now)

        verify(exactly = 1) { tasks.assignForWeek(task.id, naturalOwner.id, any()) }
    }

    @Test
    fun `seals last week's outcome before the assignment resets the task`() {
        val residents = listOf(resident("Ana"), resident("Bia"))
        val done = Task(id = UUID.randomUUID(), description = "Feita", userId = residents[0].id.toString(), isCompleted = true)
        val notDone = Task(id = UUID.randomUUID(), description = "Nao feita", userId = residents[1].id.toString(), isCompleted = false)
        stubRotation(residents, listOf(done, notDone))
        val weekStart = LocalDate.parse("2026-10-12")

        service.rotateCurrentWeek(now)

        verify(exactly = 1) { history.sealOutcome(done.id, weekStart, true, any(), false) }
        verify(exactly = 1) { history.sealOutcome(notDone.id, weekStart, false, any(), false) }
    }

    @Test
    fun `plans a week without writing anything`() {
        val residents = listOf(resident("Ana"), resident("Bia"))
        val householdTasks = tasks(2)

        val plan = service.planWeek(LocalDate.parse("2026-10-19"), householdTasks, residents, emptyMap())

        assertEquals(householdTasks, plan.map { it.first })
        assertEquals(2, plan.map { it.second.id }.toSet().size)
        verify(exactly = 0) { tasks.assignForWeek(any(), any(), any()) }
    }

    @Test
    fun `distributes every task to the only resident`() {
        val resident = resident("Ana")
        val householdTasks = tasks(4)
        every { tasks.findAll() } returns householdTasks
        every { users.findAll() } returns listOf(resident)
        every { rotationRuns.registerWeekIfAbsent(any(), now) } returns true
        every { tasks.assignForWeek(any(), resident.id, any()) } returns true

        val result = service.rotateCurrentWeek(now)

        assertTrue(result.rotated)
        assertEquals(4, result.taskCount)
        assertEquals(1, result.memberCount)
        assertEquals(mapOf("Ana" to 4), result.assignmentsPerMember)
        verify(exactly = 4) { tasks.assignForWeek(any(), resident.id, any()) }
    }

    @Test
    fun `distributes an odd number of tasks as evenly as possible`() {
        val residents = listOf(resident("Ana"), resident("Bia"))
        val householdTasks = tasks(5)
        val assignedTo = mutableListOf<UUID>()
        every { tasks.findAll() } returns householdTasks
        every { users.findAll() } returns residents
        every { rotationRuns.registerWeekIfAbsent(any(), now) } returns true
        every { tasks.assignForWeek(any(), any(), any()) } answers {
            assignedTo += secondArg<UUID>()
            true
        }

        val result = service.rotateCurrentWeek(now)

        assertEquals(5, assignedTo.size)
        val counts = residents.map { resident -> assignedTo.count { it == resident.id } }
        assertEquals(1, counts.max() - counts.min())
        assertEquals(5, result.assignmentsPerMember.values.sum())
        assertEquals(3, result.assignmentsPerMember.values.max())
        assertEquals(2, result.assignmentsPerMember.values.min())
        val expectedSundayEnd = LocalDate.parse("2026-10-12")
            .plusDays(7).atStartOfDay(ZoneId.of("America/Sao_Paulo"))
            .toInstant().minusSeconds(1)
        verify(exactly = 5) { tasks.assignForWeek(any(), any(), expectedSundayEnd) }
    }

    @Test
    fun `does not assign tasks twice in the same week`() {
        val resident = resident("Ana")
        every { tasks.findAll() } returns tasks(2)
        every { users.findAll() } returns listOf(resident)
        every { rotationRuns.registerWeekIfAbsent(any(), now) } returns false

        val result = service.rotateCurrentWeek(now)

        assertFalse(result.rotated)
        assertEquals(2, result.taskCount)
        verify(exactly = 0) { tasks.assignForWeek(any(), any(), any()) }
    }

    @Test
    fun `does not register a rotation when there are no tasks or residents`() {
        every { tasks.findAll() } returns emptyList()
        every { users.findAll() } returns emptyList()

        val result = service.rotateCurrentWeek(now)

        assertFalse(result.rotated)
        assertEquals(0, result.taskCount)
        assertEquals(0, result.memberCount)
        verify(exactly = 0) { rotationRuns.registerWeekIfAbsent(any(), any()) }
    }

    private val ANCHOR = LocalDate.of(1970, 1, 5)

    private fun stubRotation(residents: List<User>, householdTasks: List<Task>) {
        every { tasks.findAll() } returns householdTasks
        every { users.findAll() } returns residents
        every { rotationRuns.registerWeekIfAbsent(any(), now) } returns true
        every { tasks.assignForWeek(any(), any(), any()) } returns true
    }

    private fun historyEntry(taskId: UUID, owner: User, weekStart: LocalDate) = TaskRotationHistoryResponse(
        taskId = taskId,
        taskDescription = "Tarefa",
        userId = owner.id,
        userName = owner.name,
        weekStart = weekStart,
        assignedAt = now
    )

    private fun resident(name: String) = User(UUID.randomUUID(), name, "$name@example.test", true)
    private fun tasks(count: Int) = (1..count).map { index ->
        Task(id = UUID.randomUUID(), description = "Tarefa $index", userId = UUID.randomUUID().toString())
    }
}
