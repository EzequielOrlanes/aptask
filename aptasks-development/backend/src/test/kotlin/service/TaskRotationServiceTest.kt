package com.aptasks.service

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
import java.util.UUID

class TaskRotationServiceTest {
    private val tasks = mockk<TaskRepository>()
    private val users = mockk<UserRepository>()
    private val rotationRuns = mockk<TaskRotationRepository>()
    private val history = mockk<TaskRotationHistoryRepository>(relaxed = true)
    private val service = TaskRotationService(tasks, users, rotationRuns, history)
    private val now = Instant.parse("2026-10-14T12:00:00Z")

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

    private fun resident(name: String) = User(UUID.randomUUID(), name, "$name@example.test", true)
    private fun tasks(count: Int) = (1..count).map { index ->
        Task(id = UUID.randomUUID(), description = "Tarefa $index", userId = UUID.randomUUID().toString())
    }
}
