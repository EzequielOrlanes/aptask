package com.aptasks.service

import com.aptasks.dto.fine.FineRequest
import com.aptasks.model.Fine
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
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
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
        every { users.findById(senderId) } returns User(senderId, "Casa", "casa@example.test")
        every { fines.applyFine(capture(fineRequest)) } returns Fine(
            timestamp = LocalDateTime.now(), amount = BigDecimal("5.00"), sender = senderId, receiver = resident.id
        )
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
    fun `skips the fine but keeps processing other tasks when the fine sender is not configured`() {
        val resident = resident(isAtHome = true)
        val fined = overdueTask(resident)
        val postponedResident = resident(isAtHome = false)
        val postponed = overdueTask(postponedResident)
        val serviceWithoutSender = TaskService(tasks, users, rotation, fines, 5.0, "")
        every { tasks.findOverdue(now) } returns listOf(fined, postponed)
        every { users.findById(resident.id) } returns resident
        every { users.findById(postponedResident.id) } returns postponedResident
        every { tasks.postponeDeadline(postponed.id, now.plus(Duration.ofHours(24))) } just runs

        val count = serviceWithoutSender.evaluateOverdueTasks(now)

        assertEquals(1, count)
        verify(exactly = 0) { fines.applyFine(any()) }
        verify(exactly = 0) { tasks.markFineApplied(any(), any()) }
        verify(exactly = 1) { tasks.postponeDeadline(postponed.id, any()) }
    }

    @Test
    fun `skips the fine when the configured sender does not exist`() {
        val resident = resident(isAtHome = true)
        val task = overdueTask(resident)
        every { tasks.findOverdue(now) } returns listOf(task)
        every { users.findById(resident.id) } returns resident
        every { users.findById(senderId) } returns null

        assertEquals(0, service.evaluateOverdueTasks(now))

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
