package com.aptasks.service

import com.aptasks.dto.task.TaskRotationOutcome
import com.aptasks.model.Task
import com.aptasks.model.User
import com.aptasks.repository.TaskRepository
import com.aptasks.repository.TaskRotationHistoryRepository
import com.aptasks.repository.UserRepository
import com.aptasks.repository.TaskWeekExtensionRepository
import com.aptasks.model.TaskWeekExtension
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class WeeklyBoardServiceTest {
    private val tasks = mockk<TaskRepository>()
    private val users = mockk<UserRepository>()
    private val history = mockk<TaskRotationHistoryRepository>()
    private val extensions = mockk<TaskWeekExtensionRepository>()
    private val rotation = TaskRotationService(tasks, users, mockk(relaxed = true), history, extensions)
    private val service = WeeklyBoardService(tasks, users, history, rotation)
    // Quarta-feira; a semana começa na segunda 2026-10-12.
    private val now = Instant.parse("2026-10-14T12:00:00Z")
    private val ana = User(UUID.randomUUID(), "Ana", "ana@example.test", true)
    private val bia = User(UUID.randomUUID(), "Bia", "bia@example.test", true)

    init {
        every { users.findAll() } returns listOf(ana, bia)
        every { extensions.findActive(any()) } returns null
    }

    @Test
    fun `returns two previous weeks, the current one and two next weeks in order`() {
        every { tasks.findAll() } returns emptyList()
        every { history.findOutcomesByWeek(any()) } returns emptyList()

        val weeks = service.getBoard(now).weeks

        assertEquals(listOf("previous2", "previous", "current", "next", "next2"), weeks.map { it.position })
        assertEquals(
            listOf("2026-09-28", "2026-10-05", "2026-10-12", "2026-10-19", "2026-10-26").map(LocalDate::parse),
            weeks.map { it.weekStart }
        )
        assertEquals(LocalDate.parse("2026-10-18"), weeks[2].weekEnd)
    }

    @Test
    fun `reads the outcome of the week before last from the history`() {
        every { tasks.findAll() } returns emptyList()
        every { history.findOutcomesByWeek(any()) } returns emptyList()
        every { history.findOutcomesByWeek(LocalDate.parse("2026-09-28")) } returns listOf(
            TaskRotationOutcome(UUID.randomUUID(), "Lixo", ana.id, "Ana", true)
        )

        val weeks = service.getBoard(now).weeks

        assertEquals(listOf("done"), weeks[0].entries.map { it.status })
        assertTrue(weeks[1].entries.isEmpty())
    }

    @Test
    fun `maps last week's recorded outcome and marks missing records as unknown`() {
        every { tasks.findAll() } returns emptyList()
        every { history.findOutcomesByWeek(any()) } returns emptyList()
        every { history.findOutcomesByWeek(LocalDate.parse("2026-10-05")) } returns listOf(
            TaskRotationOutcome(UUID.randomUUID(), "Lixo", ana.id, "Ana", true),
            TaskRotationOutcome(UUID.randomUUID(), "Louca", bia.id, "Bia", false),
            TaskRotationOutcome(UUID.randomUUID(), "Sala", bia.id, "Bia", null)
        )

        val previous = service.getBoard(now).weeks[1].entries

        assertEquals(listOf("done", "missed", "unknown"), previous.map { it.status })
    }

    @Test
    fun `current week reflects the live task state`() {
        val done = Task(id = UUID.randomUUID(), description = "Feita", userId = ana.id.toString(), isCompleted = true)
        val open = Task(id = UUID.randomUUID(), description = "Aberta", userId = bia.id.toString())
        every { tasks.findAll() } returns listOf(done, open)
        every { history.findOutcomesByWeek(any()) } returns emptyList()

        val current = service.getBoard(now).weeks[2].entries

        assertEquals(listOf("done", "pending"), current.map { it.status })
        assertEquals(listOf("Ana", "Bia"), current.map { it.userName })
    }

    @Test
    fun `next week is a prediction and never repeats the current owner`() {
        val first = Task(id = UUID.randomUUID(), description = "A", userId = ana.id.toString())
        val second = Task(id = UUID.randomUUID(), description = "B", userId = bia.id.toString())
        every { tasks.findAll() } returns listOf(first, second)
        every { history.findOutcomesByWeek(any()) } returns emptyList()

        val next = service.getBoard(now).weeks[3].entries

        assertTrue(next.all { it.status == "upcoming" })
        assertEquals(listOf("A", "B"), next.map { it.taskDescription })
        assertTrue(next[0].userId != ana.id)
        assertTrue(next[1].userId != bia.id)
    }

    @Test
    fun `the second next week is predicted from the first one and never repeats its owner`() {
        val first = Task(id = UUID.randomUUID(), description = "A", userId = ana.id.toString())
        val second = Task(id = UUID.randomUUID(), description = "B", userId = bia.id.toString())
        every { tasks.findAll() } returns listOf(first, second)
        every { history.findOutcomesByWeek(any()) } returns emptyList()

        val weeks = service.getBoard(now).weeks
        val next = weeks[3].entries
        val next2 = weeks[4].entries

        assertTrue(next2.all { it.status == "upcoming" })
        assertEquals(listOf("A", "B"), next2.map { it.taskDescription })
        assertTrue(next2[0].userId != next[0].userId)
        assertTrue(next2[1].userId != next[1].userId)
    }

    @Test
    fun `ignores tasks whose owner no longer exists`() {
        val orphan = Task(id = UUID.randomUUID(), description = "Orfa", userId = UUID.randomUUID().toString())
        every { tasks.findAll() } returns listOf(orphan)
        every { history.findOutcomesByWeek(any()) } returns emptyList()

        assertTrue(service.getBoard(now).weeks[2].entries.isEmpty())
    }

    @Test
    fun `keeps the old week as current while its deadline is extended`() {
        val until = now.plusSeconds(3 * 24 * 3600)
        every { extensions.findActive(any()) } returns TaskWeekExtension(LocalDate.parse("2026-10-05"), until)
        every { tasks.findAll() } returns emptyList()
        every { history.findOutcomesByWeek(any()) } returns emptyList()

        val board = service.getBoard(now)

        assertEquals(
            listOf("2026-09-21", "2026-09-28", "2026-10-05", "2026-10-12", "2026-10-19").map(LocalDate::parse),
            board.weeks.map { it.weekStart }
        )
        assertEquals("current", board.weeks[2].position)
        assertEquals(until, board.extendedUntil)
    }

    @Test
    fun `has no extension when the deadline was not extended`() {
        every { tasks.findAll() } returns emptyList()
        every { history.findOutcomesByWeek(any()) } returns emptyList()

        assertEquals(null, service.getBoard(now).extendedUntil)
    }
}
