package com.aptasks.service

import com.aptasks.dto.fine.FineRequest
import com.aptasks.model.Fine
import com.aptasks.model.Task
import com.aptasks.model.User
import java.math.BigDecimal
import java.time.LocalDateTime
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Conclusão/reabertura de tarefas e as regras de multa tardia que dependem
 * do estado da tarefa e do morador (presença em casa).
 */
class TaskServiceStatusTest {
    private val tasks = mockk<TaskRepository>()
    private val users = mockk<UserRepository>()
    private val rotation = mockk<TaskRotationService>()
    private val fines = mockk<FineService>()
    private val senderId = UUID.randomUUID()
    private val service = TaskService(tasks, users, rotation, fines, 5.0, senderId.toString())
    private val home = User(UUID.randomUUID(), "Ana", "ana@example.test", isAtHome = true)
    private val away = User(UUID.randomUUID(), "Bia", "bia@example.test", isAtHome = false)

    // ---------- toggleStatus / setCompletion ----------

    @Test
    fun `toggleStatus completes an on-time task without applying a fine`() {
        val task = task(home, dueAt = Instant.now().plus(Duration.ofDays(1)))
        every { tasks.findById(task.id) } returns task
        every { tasks.update(any()) } answers { firstArg() }

        service.toggleStatus(task.id)

        assertTrue(task.isCompleted)
        assertNotNull(task.decisionAt)
        verify(exactly = 0) { users.findById(any()) }
        verify(exactly = 0) { fines.applyFine(any()) }
    }

    @Test
    fun `toggleStatus called twice reopens the task`() {
        val task = task(home, dueAt = Instant.now().plus(Duration.ofDays(1)))
        every { tasks.findById(task.id) } returns task
        every { tasks.update(any()) } answers { firstArg() }

        service.toggleStatus(task.id)
        service.toggleStatus(task.id)

        assertFalse(task.isCompleted)
    }

    @Test
    fun `late completion by a resident at home applies the fine immediately`() {
        val task = task(home, dueAt = Instant.now().minus(Duration.ofHours(1)))
        every { tasks.findById(task.id) } returns task
        every { tasks.update(any()) } answers { firstArg() }
        every { users.findById(home.id) } returns home
        every { users.findById(senderId) } returns User(senderId, "Casa", "casa@example.test")
        val fineRequest = slot<FineRequest>()
        every { fines.applyFine(capture(fineRequest)) } returns fineStub()
        every { tasks.markFineApplied(task.id, any()) } just runs

        service.toggleStatus(task.id)

        assertEquals(5.0, fineRequest.captured.amount)
        assertEquals(senderId.toString(), fineRequest.captured.sender)
        assertEquals(home.id.toString(), fineRequest.captured.receiver)
        verify(exactly = 1) { tasks.markFineApplied(task.id, any()) }
    }

    @Test
    fun `late completion by a resident away records the decision but no fine`() {
        val task = task(away, dueAt = Instant.now().minus(Duration.ofHours(1)))
        every { tasks.findById(task.id) } returns task
        every { tasks.update(any()) } answers { firstArg() }
        every { users.findById(away.id) } returns away

        service.toggleStatus(task.id)

        assertTrue(task.isCompleted)
        verify(exactly = 0) { fines.applyFine(any()) }
        verify(exactly = 0) { tasks.markFineApplied(any(), any()) }
    }

    @Test
    fun `a task already fined is not fined again`() {
        val task = task(
            home,
            dueAt = Instant.now().minus(Duration.ofHours(1)),
            fineAppliedAt = Instant.now().minus(Duration.ofMinutes(30))
        )
        every { tasks.findById(task.id) } returns task
        every { tasks.update(any()) } answers { firstArg() }

        service.toggleStatus(task.id)

        verify(exactly = 0) { users.findById(any()) }
        verify(exactly = 0) { fines.applyFine(any()) }
    }

    @Test
    fun `setCompletion returns the refreshed weekly view of the task`() {
        val task = task(home, dueAt = Instant.now().plus(Duration.ofDays(1)))
        every { tasks.findById(task.id) } returns task
        every { tasks.update(any()) } answers { firstArg() }
        every { users.findAll() } returns listOf(home)
        every { tasks.findAll() } returns listOf(task)

        val response = service.setCompletion(task.id, done = true)

        assertEquals(task.id, response.id)
        assertEquals("done", response.status)
        assertTrue(response.decisionRecorded)
    }

    @Test
    fun `setCompletion throws when the task does not exist`() {
        val id = UUID.randomUUID()
        every { tasks.findById(id) } returns null

        assertThrows<com.aptasks.exception.TaskNotFoundException> {
            service.setCompletion(id, done = true)
        }
    }

    // ---------- evaluateOverdueTasks: caminhos de erro ----------

    @Test
    fun `evaluateOverdueTasks skips a task whose responsible id is not a UUID and keeps going`() {
        val broken = task(home, dueAt = Instant.now().minusSeconds(60)).copy(userId = "nao-uuid")
        val ok = task(away, dueAt = Instant.now().minusSeconds(60))
        every { tasks.findOverdue(any()) } returns listOf(broken, ok)
        every { users.findById(away.id) } returns away
        every { tasks.postponeDeadline(ok.id, any()) } just runs

        val postponed = service.evaluateOverdueTasks(Instant.now())

        assertEquals(1, postponed)
        verify(exactly = 0) { fines.applyFine(any()) }
        verify(exactly = 0) { tasks.postponeDeadline(broken.id, any()) }
    }

    @Test
    fun `evaluateOverdueTasks skips a task whose responsible resident does not exist`() {
        val task = task(home, dueAt = Instant.now().minusSeconds(60))
        every { tasks.findOverdue(any()) } returns listOf(task)
        every { users.findById(home.id) } returns null

        assertEquals(0, service.evaluateOverdueTasks(Instant.now()))

        verify(exactly = 0) { tasks.postponeDeadline(any(), any()) }
        verify(exactly = 0) { fines.applyFine(any()) }
    }

    // ---------- helpers ----------

    private fun fineStub() = Fine(
        timestamp = LocalDateTime.now(),
        amount = BigDecimal("5.00"),
        sender = senderId,
        receiver = home.id
    )

    private fun task(resident: User, dueAt: Instant? = null, fineAppliedAt: Instant? = null) = Task(
        id = UUID.randomUUID(),
        description = "Tarefa de ${resident.name}",
        userId = resident.id.toString(),
        dueAt = dueAt,
        fineAppliedAt = fineAppliedAt
    )
}
