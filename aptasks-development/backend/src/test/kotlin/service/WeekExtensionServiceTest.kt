package com.aptasks.service

import com.aptasks.model.Task
import com.aptasks.repository.TaskRepository
import com.aptasks.dto.task.TaskRotationOutcome
import com.aptasks.repository.TaskRotationHistoryRepository
import com.aptasks.repository.TaskRotationRepository
import com.aptasks.repository.TaskWeekExtensionRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class WeekExtensionServiceTest {
    private val tasks = mockk<TaskRepository>()
    private val fines = mockk<FineService>()
    private val rotationRuns = mockk<TaskRotationRepository>()
    private val extensions = mockk<TaskWeekExtensionRepository>()
    private val history = mockk<TaskRotationHistoryRepository>()
    private val rotation = mockk<TaskRotationService>()
    private val service = WeekExtensionService(tasks, fines, rotationRuns, extensions, history, rotation)
    private val now = Instant.parse("2026-10-20T12:00:00Z")
    private val until = Instant.parse("2026-10-22T21:00:00Z")
    private val weekStart = LocalDate.parse("2026-10-12")

    init {
        every { rotationRuns.latestWeek() } returns weekStart
        every { tasks.postponeDeadline(any(), any()) } just Runs
        every { tasks.clearFineApplied(any()) } just Runs
        every { extensions.upsert(any(), any(), any()) } just Runs
        every { extensions.deleteWeeksAfter(any()) } just Runs
        every { history.clearOutcomes(any()) } just Runs
        every { history.deleteWeeksAfter(any()) } just Runs
        every { rotationRuns.deleteWeeksAfter(any()) } just Runs
        every { tasks.restoreForReopen(any(), any(), any(), any(), any(), any()) } just Runs
        every { rotation.currentWeekStart(any()) } returns weekStart
    }

    @Test
    fun `extends every task of the week and registers the extension`() {
        val first = task(fineAppliedAt = null)
        val second = task(fineAppliedAt = null)
        every { tasks.findAll() } returns listOf(first, second)

        val result = service.extendCurrentWeek(until, now)

        verify(exactly = 1) { tasks.postponeDeadline(first.id, until) }
        verify(exactly = 1) { tasks.postponeDeadline(second.id, until) }
        verify(exactly = 1) { extensions.upsert(weekStart, until, now) }
        assertEquals(WeekExtensionResult(weekStart, until, 2, 0, 0), result)
    }

    @Test
    fun `cancels the week's automatic fines and frees the task`() {
        val appliedAt = now.minusSeconds(3600)
        val fined = task(fineAppliedAt = appliedAt)
        every { tasks.findAll() } returns listOf(fined)
        every { fines.cancelAutomaticFines(UUID.fromString(fined.userId), appliedAt) } returns CancelledFines(1, 0)

        val result = service.extendCurrentWeek(until, now)

        verify(exactly = 1) { tasks.clearFineApplied(fined.id) }
        assertEquals(1, result.finesCancelled)
        assertEquals(0, result.finesKeptPaid)
    }

    @Test
    fun `keeps a paid fine and does not free the task to be fined again`() {
        val appliedAt = now.minusSeconds(3600)
        val fined = task(fineAppliedAt = appliedAt)
        every { tasks.findAll() } returns listOf(fined)
        every { fines.cancelAutomaticFines(any(), appliedAt) } returns CancelledFines(0, 1)

        val result = service.extendCurrentWeek(until, now)

        verify(exactly = 0) { tasks.clearFineApplied(any()) }
        assertEquals(1, result.finesKeptPaid)
    }

    @Test
    fun `rejects a deadline that is not in the future`() {
        every { tasks.findAll() } returns emptyList()

        assertThrows<IllegalArgumentException> { service.extendCurrentWeek(now.minusSeconds(1), now) }
        verify(exactly = 0) { extensions.upsert(any(), any(), any()) }
    }

    @Test
    fun `rejects the extension when no week was started yet`() {
        every { rotationRuns.latestWeek() } returns null

        assertThrows<IllegalArgumentException> { service.extendCurrentWeek(until, now) }
    }

    // ---- reabrir a semana anterior ----

    private val previousWeek = weekStart.minusWeeks(1)
    private val rotatedAt = Instant.parse("2026-10-12T03:10:00Z")
    private val weekDeadline = Instant.parse("2026-10-12T02:59:59Z")

    private fun reopenSetup(rows: List<TaskRotationOutcome>, live: List<Task>) {
        every { history.findOutcomesByWeek(previousWeek) } returns rows
        every { rotation.weekDeadline(previousWeek) } returns weekDeadline
        every { rotationRuns.executedAt(weekStart) } returns rotatedAt
        every { tasks.findAll() } returns live
        every { tasks.clearFineApplied(any()) } just Runs
        every { fines.cancelFinesBetween(any(), any(), any()) } returns CancelledFines(0, 0)
    }

    private fun outcome(task: Task, owner: UUID, done: Boolean?, fined: Boolean? = null, decidedAt: Instant? = null) =
        TaskRotationOutcome(task.id, task.description, owner, "Morador", done, decidedAt, fined)

    @Test
    fun `reopening the previous week gives the tasks back to their owners with the new deadline`() {
        val ana = UUID.randomUUID()
        val bia = UUID.randomUUID()
        val first = task(fineAppliedAt = null)
        val second = task(fineAppliedAt = null)
        val decided = now.minusSeconds(86400 * 3)
        reopenSetup(listOf(outcome(first, ana, true, false, decided), outcome(second, bia, false, true)), listOf(first, second))

        val result = service.reopenPreviousWeek(until, now)

        verify(exactly = 1) { tasks.restoreForReopen(first.id, ana, until, true, decided, null) }
        verify(exactly = 1) { tasks.restoreForReopen(second.id, bia, until, false, null, null) }
        assertEquals(previousWeek, result.weekStart)
    }

    @Test
    fun `reopening undoes the later rotation and clears the reopened week's outcome`() {
        val owner = UUID.randomUUID()
        val only = task(fineAppliedAt = null)
        reopenSetup(listOf(outcome(only, owner, true)), listOf(only))

        service.reopenPreviousWeek(until, now)

        verify(exactly = 1) { history.clearOutcomes(previousWeek) }
        verify(exactly = 1) { history.deleteWeeksAfter(previousWeek) }
        verify(exactly = 1) { rotationRuns.deleteWeeksAfter(previousWeek) }
        verify(exactly = 1) { extensions.deleteWeeksAfter(previousWeek) }
        verify(exactly = 1) { extensions.upsert(previousWeek, until, now) }
    }

    @Test
    fun `reopening cancels the fines applied when the week expired, once per resident`() {
        val ana = UUID.randomUUID()
        val first = task(fineAppliedAt = null)
        val second = task(fineAppliedAt = null)
        reopenSetup(listOf(outcome(first, ana, false, true), outcome(second, ana, false, true)), listOf(first, second))
        every { fines.cancelFinesBetween(ana, weekDeadline, rotatedAt) } returns CancelledFines(1, 0)

        val result = service.reopenPreviousWeek(until, now)

        verify(exactly = 1) { fines.cancelFinesBetween(ana, weekDeadline, rotatedAt) }
        assertEquals(1, result.finesCancelled)
    }

    @Test
    fun `reopening does not touch fines of residents who were not fined`() {
        val ana = UUID.randomUUID()
        val only = task(fineAppliedAt = null)
        reopenSetup(listOf(outcome(only, ana, true, fined = false)), listOf(only))

        service.reopenPreviousWeek(until, now)

        verify(exactly = 0) { fines.cancelFinesBetween(any(), any(), any()) }
    }

    @Test
    fun `reopening keeps the fine mark when a paid fine stands, so nobody is fined twice`() {
        val ana = UUID.randomUUID()
        val only = task(fineAppliedAt = null)
        reopenSetup(listOf(outcome(only, ana, false, true)), listOf(only))
        every { fines.cancelFinesBetween(ana, weekDeadline, rotatedAt) } returns CancelledFines(0, 1)

        val result = service.reopenPreviousWeek(until, now)

        verify(exactly = 1) { tasks.restoreForReopen(only.id, ana, until, false, null, weekDeadline) }
        assertEquals(1, result.finesKeptPaid)
    }

    @Test
    fun `reopening only extends the deadline of tasks created after that week`() {
        val ana = UUID.randomUUID()
        val old = task(fineAppliedAt = null)
        val created = task(fineAppliedAt = null)
        reopenSetup(listOf(outcome(old, ana, true)), listOf(old, created))
        every { tasks.postponeDeadline(any(), any()) } just Runs

        service.reopenPreviousWeek(until, now)

        verify(exactly = 1) { tasks.postponeDeadline(created.id, until) }
        verify(exactly = 0) { tasks.restoreForReopen(created.id, any(), any(), any(), any(), any()) }
    }

    @Test
    fun `reopening is refused when there is no record of the previous week`() {
        reopenSetup(emptyList(), emptyList())

        assertThrows<IllegalArgumentException> { service.reopenPreviousWeek(until, now) }
        verify(exactly = 0) { rotationRuns.deleteWeeksAfter(any()) }
    }

    @Test
    fun `reopening is refused for a deadline in the past`() {
        reopenSetup(emptyList(), emptyList())

        assertThrows<IllegalArgumentException> { service.reopenPreviousWeek(now.minusSeconds(1), now) }
    }

    private fun task(fineAppliedAt: Instant?) = Task(
        id = UUID.randomUUID(),
        description = "Tarefa",
        userId = UUID.randomUUID().toString(),
        dueAt = now.minusSeconds(86400),
        fineAppliedAt = fineAppliedAt
    )
}
