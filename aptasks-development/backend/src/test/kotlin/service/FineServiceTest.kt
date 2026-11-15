package com.aptasks.service

import com.aptasks.dto.fine.FineRequest
import com.aptasks.model.Fine
import com.aptasks.repository.FineRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

/** Testes unitários de FineService: validação dos UUIDs e montagem da entidade Fine. */
class FineServiceTest {
    private val repository = mockk<FineRepository>()
    private val service = FineService(repository)

    @Test
    fun `persists an unpaid fine with parsed UUIDs and the requested amount`() {
        val sender = UUID.randomUUID()
        val receiver = UUID.randomUUID()
        val timestamp = LocalDateTime.of(2026, 10, 15, 12, 0)
        val inserted = slot<Fine>()
        every { repository.insert(capture(inserted)) } answers { inserted.captured }

        service.applyFine(
            FineRequest(amount = 5.5, sender = sender.toString(), receiver = receiver.toString(), timestamp = timestamp)
        )

        val fine = inserted.captured
        assertEquals(sender, fine.sender)
        assertEquals(receiver, fine.receiver)
        assertEquals(5.5f, fine.amount)
        assertEquals(timestamp.toString(), fine.timestamp)
        assertFalse(fine.isPaid)
        verify(exactly = 1) { repository.insert(any()) }
    }

    @Test
    fun `rejects an invalid sender UUID without touching the repository`() {
        val error = assertThrows<IllegalArgumentException> {
            service.applyFine(FineRequest(amount = 5.0, sender = "nao-é-uuid", receiver = UUID.randomUUID().toString()))
        }

        assertEquals("UUID do remetente (sender) é inválido.", error.message)
        verify(exactly = 0) { repository.insert(any()) }
    }

    @Test
    fun `rejects an invalid receiver UUID without touching the repository`() {
        val error = assertThrows<IllegalArgumentException> {
            service.applyFine(FineRequest(amount = 5.0, sender = UUID.randomUUID().toString(), receiver = "123"))
        }

        assertEquals("UUID do destinatário (receiver) é inválido.", error.message)
        verify(exactly = 0) { repository.insert(any()) }
    }
}
