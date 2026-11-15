package com.aptasks.service

import com.aptasks.dto.fine.FineRequest
import com.aptasks.model.Task
import com.aptasks.model.User
import com.aptasks.repository.TaskRepository
import com.aptasks.repository.TaskRotationRepository
import com.aptasks.repository.UserRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class TaskServiceOverdueTest {
    private val tasks = mockk<TaskRepository>()
    private val users = mockk<UserRepository>()
    private val rotation = mockk<TaskRotationService>()
    private val fines = mockk<FineService>()
    private val senderId = UUID.randomUUID()
    private val service = TaskService(tasks, users, rotation, fines, 5.0, senderId.toString())
    private val now = Instant.parse("2026-10-15T12:00:00Z")

    @Test
    fun `applies a fine and marks an overdue task when its resident is home`() {
        val resident = resident(isAtHome = true)
        val task = overdueTask(resident)
        val fineRequest = slot<FineRequest>()
        every { tasks.findOverdue(now) } returns listOf(task)
        every { users.findById(resident.id) } returns resident
        every { fines.applyFine(capture(fineRequest)) } just runs
        every { tasks.markFineApplied(task.id, now) } just runs

        val postponed = service.evaluateOverdueTasks(now)

        assertEquals(0, postponed)
        assertEquals(5.0, fineRequest.captured.amount)
        assertEquals(senderId.toString(), fineRequest.captured.sender)
        assertEquals(resident.id.toString(), fineRequest.captured.receiver)
        verify(exactly = 1) { tasks.markFineApplied(task.id, now) }
        verify(exactly = 0) { tasks.postponeDeadline(any(), any()) }
    }

    @Test
    fun `postpones an overdue task by 24 hours when its resident is away`() {
        val resident = resident(isAtHome = false)
        val task = overdueTask(resident)
        every { tasks.findOverdue(now) } returns listOf(task)
        every { users.findById(resident.id) } returns resident
        every { tasks.postponeDeadline(task.id, now.plus(Duration.ofHours(24))) } just runs

        val postponed = service.evaluateOverdueTasks(now)

        assertEquals(1, postponed)
        verify(exactly = 1) { tasks.postponeDeadline(task.id, now.plus(Duration.ofHours(24))) }
        verify(exactly = 0) { fines.applyFine(any()) }
        verify(exactly = 0) { tasks.markFineApplied(any(), any()) }
    }

    @Test
    fun `does nothing when there are no overdue tasks`() {
        every { tasks.findOverdue(now) } returns emptyList()

        assertEquals(0, service.evaluateOverdueTasks(now))
        verify(exactly = 0) { users.findById(any()) }
        verify(exactly = 0) { fines.applyFine(any()) }
    }

    @Test
    fun `fails clearly when the fine sender is not configured`() {
        val resident = resident(isAtHome = true)
        val task = overdueTask(resident)
        val serviceWithoutSender = TaskService(tasks, users, rotation, fines, 5.0, "")
        every { tasks.findOverdue(now) } returns listOf(task)
        every { users.findById(resident.id) } returns resident

        val error = assertThrows(IllegalStateException::class.java) {
            serviceWithoutSender.evaluateOverdueTasks(now)
        }

        assertEquals(true, error.message?.contains("TASK_FINE_SENDER_ID"))
        verify(exactly = 0) { fines.applyFine(any()) }
        verify(exactly = 0) { tasks.markFineApplied(any(), any()) }
    }

    private fun resident(isAtHome: Boolean) = User(UUID.randomUUID(), "Morador", "morador@example.test", isAtHome)
    private fun overdueTask(resident: User) = Task(
        id = UUID.randomUUID(),
        description = "Limpar a cozinha",
        userId = resident.id.toString(),
        dueAt = now.minusSeconds(60)
    )
}
