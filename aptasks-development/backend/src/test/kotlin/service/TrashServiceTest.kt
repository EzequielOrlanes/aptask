package com.aptasks.service

import com.aptasks.dto.ResidentResponse
import com.aptasks.exception.TrashConflictException
import com.aptasks.exception.TrashForbiddenException
import com.aptasks.model.TrashStatus
import com.aptasks.model.User
import com.aptasks.repository.TrashRepository
import com.aptasks.repository.UserRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

/** Testes unitários de TrashService: estado, validações e transições vazio, meio cheio, cheio e esvaziado. */
class TrashServiceTest {
    private val trashRepository = mockk<TrashRepository>()
    private val userRepository = mockk<UserRepository>()
    private val service = TrashService(trashRepository, userRepository)
    private val now: Instant = Instant.parse("2026-11-15T10:00:00Z")

    @BeforeEach
    fun setUp() {
        every { trashRepository.ensureRow() } just runs
        every { trashRepository.setResponsible(any(), any()) } just runs
        every { trashRepository.insertEvent(any(), any(), any()) } just runs
    }

    @Test
    fun `returns an empty status when the trash row is missing`() {
        every { trashRepository.findStatus() } returns TrashStatus.EMPTY

        val status = service.getTodayStatus()

        assertNull(status.responsible)
        assertEquals("EMPTY", status.fillLevel)
        assertFalse(status.isFull)
        assertNull(status.reportedAt)
        assertNull(status.reportedBy)
    }

    @Test
    fun `status maps fillLevel and isFull for HALF`() {
        every { trashRepository.findStatus() } returns TrashStatus(null, "HALF", now, null)

        val status = service.getTodayStatus()

        assertEquals("HALF", status.fillLevel)
        assertFalse(status.isFull)
    }

    @Test
    fun `status maps fillLevel and isFull for FULL`() {
        every { trashRepository.findStatus() } returns TrashStatus(null, "FULL", now, null)

        val status = service.getTodayStatus()

        assertEquals("FULL", status.fillLevel)
        assertTrue(status.isFull)
    }

    @Test
    fun `maps responsible and reporter to resident responses`() {
        val responsible = user(name = "Ana", isAtHome = true)
        val reporter = user(name = "Bruno", isAtHome = false)
        every { trashRepository.findStatus() } returns TrashStatus(
            responsibleUserId = responsible.id,
            fillLevel = "FULL",
            reportedAt = now,
            reportedByUserId = reporter.id
        )
        every { userRepository.findById(responsible.id) } returns responsible
        every { userRepository.findById(reporter.id) } returns reporter

        val status = service.getTodayStatus()

        assertEquals(ResidentResponse(responsible.id, "Ana", true), status.responsible)
        assertEquals(ResidentResponse(reporter.id, "Bruno", false), status.reportedBy)
        assertEquals("FULL", status.fillLevel)
        assertTrue(status.isFull)
        assertEquals(now, status.reportedAt)
    }

    @Test
    fun `returns null reporter when the reporting user no longer exists`() {
        val reporterId = UUID.randomUUID()
        every { trashRepository.findStatus() } returns TrashStatus(
            responsibleUserId = null,
            fillLevel = "FULL",
            reportedAt = now,
            reportedByUserId = reporterId
        )
        every { userRepository.findById(reporterId) } returns null

        val status = service.getTodayStatus()

        assertNull(status.reportedBy)
        assertNull(status.responsible)
    }

    @Test
    fun `assignResponsible persists the resident and records RESPONSIBLE_CHANGED`() {
        val resident = user()
        every { userRepository.findById(resident.id) } returns resident
        every { trashRepository.findStatus() } returns TrashStatus(resident.id, "EMPTY", null, null)

        val status = service.assignResponsible(resident.id.toString(), now)

        verify(exactly = 1) { trashRepository.ensureRow() }
        verify(exactly = 1) { trashRepository.setResponsible(resident.id, now) }
        verify(exactly = 1) { trashRepository.insertEvent("RESPONSIBLE_CHANGED", resident.id, now) }
        assertEquals(resident.id, status.responsible?.id)
    }

    @Test
    fun `assignResponsible rejects a blank id without writes`() {
        val error = assertThrows<IllegalArgumentException> {
            service.assignResponsible("   ", now)
        }

        assertEquals("O identificador do morador é obrigatório.", error.message)
        verify(exactly = 0) { trashRepository.ensureRow() }
        verify(exactly = 0) { trashRepository.setResponsible(any(), any()) }
        verify(exactly = 0) { trashRepository.insertEvent(any(), any(), any()) }
    }

    @Test
    fun `assignResponsible rejects a malformed UUID without writes`() {
        val error = assertThrows<IllegalArgumentException> {
            service.assignResponsible("nao-é-uuid", now)
        }

        assertEquals("O identificador do morador é inválido.", error.message)
        verify(exactly = 0) { trashRepository.setResponsible(any(), any()) }
        verify(exactly = 0) { trashRepository.insertEvent(any(), any(), any()) }
    }

    @Test
    fun `assignResponsible rejects an unknown resident without writes`() {
        val unknownId = UUID.randomUUID()
        every { userRepository.findById(unknownId) } returns null

        val error = assertThrows<IllegalArgumentException> {
            service.assignResponsible(unknownId.toString(), now)
        }

        assertEquals("Morador não encontrado.", error.message)
        verify(exactly = 0) { trashRepository.ensureRow() }
        verify(exactly = 0) { trashRepository.setResponsible(any(), any()) }
        verify(exactly = 0) { trashRepository.insertEvent(any(), any(), any()) }
    }

    @Test
    fun `reportHalf sets HALF from EMPTY and records REPORTED_HALF`() {
        val resident = user()
        every { userRepository.findById(resident.id) } returns resident
        every { trashRepository.findStatus() } returnsMany listOf(
            TrashStatus.EMPTY,
            TrashStatus(null, "HALF", now, resident.id)
        )
        every { trashRepository.markHalfIfEmpty(resident.id, now) } returns true

        val status = service.reportHalf(resident.id.toString(), now)

        verify(exactly = 1) { trashRepository.markHalfIfEmpty(resident.id, now) }
        verify(exactly = 1) { trashRepository.insertEvent("REPORTED_HALF", resident.id, now) }
        assertEquals("HALF", status.fillLevel)
        assertEquals(resident.id, status.reportedBy?.id)
    }

    @Test
    fun `reportHalf on HALF returns the current status and writes nothing`() {
        val resident = user()
        every { userRepository.findById(resident.id) } returns resident
        every { trashRepository.findStatus() } returns TrashStatus(null, "HALF", now, resident.id)

        val response = service.reportHalf(resident.id.toString(), now)

        assertEquals("HALF", response.fillLevel)
        verify(exactly = 0) { trashRepository.ensureRow() }
        verify(exactly = 0) { trashRepository.markHalfIfEmpty(any(), any()) }
        verify(exactly = 0) { trashRepository.insertEvent(any(), any(), any()) }
    }

    @Test
    fun `reportHalf on FULL returns the current status and writes nothing`() {
        val resident = user()
        every { userRepository.findById(resident.id) } returns resident
        every { trashRepository.findStatus() } returns TrashStatus(null, "FULL", now, resident.id)

        val response = service.reportHalf(resident.id.toString(), now)

        assertEquals("FULL", response.fillLevel)
        verify(exactly = 0) { trashRepository.markHalfIfEmpty(any(), any()) }
        verify(exactly = 0) { trashRepository.insertEvent(any(), any(), any()) }
    }

    @Test
    fun `reportFull sets FULL from EMPTY and records REPORTED_FULL`() {
        val resident = user()
        every { userRepository.findById(resident.id) } returns resident
        every { trashRepository.findStatus() } returnsMany listOf(
            TrashStatus.EMPTY,
            TrashStatus(null, "FULL", now, resident.id)
        )
        every { trashRepository.markFullIfNotFull(resident.id, now) } returns true

        val status = service.reportFull(resident.id.toString(), now)

        verify(exactly = 1) { trashRepository.markFullIfNotFull(resident.id, now) }
        verify(exactly = 1) { trashRepository.insertEvent("REPORTED_FULL", resident.id, now) }
        assertTrue(status.isFull)
        assertEquals(resident.id, status.reportedBy?.id)
    }

    @Test
    fun `reportFull sets FULL from HALF and records REPORTED_FULL`() {
        val resident = user()
        every { userRepository.findById(resident.id) } returns resident
        every { trashRepository.findStatus() } returnsMany listOf(
            TrashStatus(null, "HALF", now, resident.id),
            TrashStatus(null, "FULL", now, resident.id)
        )
        every { trashRepository.markFullIfNotFull(resident.id, now) } returns true

        val status = service.reportFull(resident.id.toString(), now)

        verify(exactly = 1) { trashRepository.markFullIfNotFull(resident.id, now) }
        verify(exactly = 1) { trashRepository.insertEvent("REPORTED_FULL", resident.id, now) }
        assertEquals("FULL", status.fillLevel)
    }

    @Test
    fun `reportFull returns the current status without writing when the trash is already full`() {
        val resident = user()
        every { userRepository.findById(resident.id) } returns resident
        every { trashRepository.findStatus() } returns TrashStatus(
            responsibleUserId = null, fillLevel = "FULL", reportedAt = now, reportedByUserId = resident.id
        )

        val response = service.reportFull(resident.id.toString(), now)

        assertTrue(response.isFull)
        verify(exactly = 0) { trashRepository.ensureRow() }
        verify(exactly = 0) { trashRepository.markFullIfNotFull(any(), any()) }
        verify(exactly = 0) { trashRepository.insertEvent(any(), any(), any()) }
    }

    @Test
    fun `markEmptied from HALF clears the trash and records MARKED_EMPTIED`() {
        val resident = user()
        every { userRepository.findById(resident.id) } returns resident
        every { trashRepository.markEmptiedIfNotEmpty(now) } returns true
        every { trashRepository.findStatus() } returnsMany listOf(
            TrashStatus(resident.id, "HALF", now, resident.id),
            TrashStatus(resident.id, "EMPTY", null, null)
        )

        val status = service.markEmptied(resident.id.toString(), now)

        verify(exactly = 1) { trashRepository.markEmptiedIfNotEmpty(now) }
        verify(exactly = 1) { trashRepository.insertEvent("MARKED_EMPTIED", resident.id, now) }
        assertEquals("EMPTY", status.fillLevel)
        assertFalse(status.isFull)
    }

    @Test
    fun `markEmptied from FULL clears the trash and records MARKED_EMPTIED`() {
        val resident = user()
        every { userRepository.findById(resident.id) } returns resident
        every { trashRepository.markEmptiedIfNotEmpty(now) } returns true
        every { trashRepository.findStatus() } returnsMany listOf(
            TrashStatus(resident.id, "FULL", now, resident.id),
            TrashStatus.EMPTY
        )

        val status = service.markEmptied(resident.id.toString(), now)

        verify(exactly = 1) { trashRepository.markEmptiedIfNotEmpty(now) }
        verify(exactly = 1) { trashRepository.insertEvent("MARKED_EMPTIED", resident.id, now) }
        assertEquals("EMPTY", status.fillLevel)
    }

    @Test
    fun `markEmptied on an EMPTY trash throws a conflict and writes nothing`() {
        val resident = user()
        every { userRepository.findById(resident.id) } returns resident
        every { trashRepository.findStatus() } returns TrashStatus(resident.id, "EMPTY", null, null)

        val error = assertThrows<TrashConflictException> {
            service.markEmptied(resident.id.toString(), now)
        }

        assertEquals("O lixo já está vazio.", error.message)
        verify(exactly = 0) { trashRepository.ensureRow() }
        verify(exactly = 0) { trashRepository.markEmptiedIfNotEmpty(any()) }
        verify(exactly = 0) { trashRepository.insertEvent(any(), any(), any()) }
    }

    @Test
    fun `markEmptied rejects a resident who is not the current responsible and writes nothing`() {
        val resident = user()
        val responsible = user(name = "Responsável")
        every { userRepository.findById(resident.id) } returns resident
        every { trashRepository.findStatus() } returns TrashStatus(responsible.id, "FULL", now, responsible.id)

        val error = assertThrows<TrashForbiddenException> {
            service.markEmptied(resident.id.toString(), now)
        }

        assertEquals("Só o responsável pelo lixo pode marcar como esvaziado.", error.message)
        verify(exactly = 0) { trashRepository.markEmptiedIfNotEmpty(any()) }
        verify(exactly = 0) { trashRepository.insertEvent(any(), any(), any()) }
    }

    private fun user(name: String = "Morador", isAtHome: Boolean = false) = User(
        id = UUID.randomUUID(),
        name = name,
        email = "$name@example.com".lowercase(),
        isAtHome = isAtHome
    )
}
