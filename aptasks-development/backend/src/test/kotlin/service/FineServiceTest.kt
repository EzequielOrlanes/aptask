package com.aptasks.service

import com.aptasks.dto.fine.FineRequest
import com.aptasks.dto.fine.FineResponse
import com.aptasks.exception.FineNotFoundException
import com.aptasks.model.Fine
import com.aptasks.model.User
import com.aptasks.repository.FineRepository
import com.aptasks.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/** Testes unitários de FineService: validações, persistência, listagem e pagamento. */
class FineServiceTest {
    private val fines = mockk<FineRepository>()
    private val users = mockk<UserRepository>()
    private val service = FineService(fines, users)
    private val sender = User(UUID.randomUUID(), "Ana", "ana@example.test")
    private val receiver = User(UUID.randomUUID(), "Bia", "bia@example.test")
    private val timestamp = LocalDateTime.of(2026, 10, 15, 12, 0)

    @BeforeEach
    fun setUp() {
        every { users.findById(sender.id) } returns sender
        every { users.findById(receiver.id) } returns receiver
        every { fines.insert(any()) } answers { firstArg() }
    }

    @Test
    fun `persists an unpaid fine with parsed UUIDs and a two-decimal amount`() {
        val inserted = slot<Fine>()
        every { fines.insert(capture(inserted)) } answers { inserted.captured }

        val result = service.applyFine(request(amount = 5.5))

        val fine = inserted.captured
        assertEquals(sender.id, fine.sender)
        assertEquals(receiver.id, fine.receiver)
        assertEquals(BigDecimal("5.50"), fine.amount)
        assertEquals(timestamp, fine.timestamp)
        assertFalse(fine.isPaid)
        assertEquals(fine, result)
    }

    @Test
    fun `rounds the amount to cents using half-up`() {
        val inserted = slot<Fine>()
        every { fines.insert(capture(inserted)) } answers { inserted.captured }

        service.applyFine(request(amount = 10.005))

        assertEquals(BigDecimal("10.01"), inserted.captured.amount)
    }

    @Test
    fun `rejects an invalid sender UUID without touching the repository`() {
        val error = assertThrows<IllegalArgumentException> {
            service.applyFine(FineRequest(amount = 5.0, sender = "nao-é-uuid", receiver = receiver.id.toString()))
        }

        assertEquals("UUID do remetente (sender) é inválido.", error.message)
        verify(exactly = 0) { fines.insert(any()) }
    }

    @Test
    fun `rejects an invalid receiver UUID without touching the repository`() {
        val error = assertThrows<IllegalArgumentException> {
            service.applyFine(FineRequest(amount = 5.0, sender = sender.id.toString(), receiver = "123"))
        }

        assertEquals("UUID do destinatário (receiver) é inválido.", error.message)
        verify(exactly = 0) { fines.insert(any()) }
    }

    @Test
    fun `rejects a fine where sender and receiver are the same resident`() {
        val error = assertThrows<IllegalArgumentException> {
            service.applyFine(request(sender = sender.id, receiver = sender.id))
        }

        assertTrue(error.message!!.contains("moradores diferentes"))
        verify(exactly = 0) { fines.insert(any()) }
    }

    @Test
    fun `rejects zero, negative, non-finite and oversized amounts`() {
        listOf(0.0, -3.0, 0.004, Double.NaN, Double.POSITIVE_INFINITY, 100_000_000.0).forEach { amount ->
            assertThrows<IllegalArgumentException>("amount=$amount") { service.applyFine(request(amount = amount)) }
        }
        verify(exactly = 0) { fines.insert(any()) }
    }

    @Test
    fun `rejects a sender that does not exist`() {
        val unknown = UUID.randomUUID()
        every { users.findById(unknown) } returns null

        val error = assertThrows<IllegalArgumentException> {
            service.applyFine(request(sender = unknown))
        }

        assertEquals("O remetente da multa não foi encontrado.", error.message)
        verify(exactly = 0) { fines.insert(any()) }
    }

    @Test
    fun `rejects a receiver that does not exist`() {
        val unknown = UUID.randomUUID()
        every { users.findById(unknown) } returns null

        val error = assertThrows<IllegalArgumentException> {
            service.applyFine(request(receiver = unknown))
        }

        assertEquals("O destinatário da multa não foi encontrado.", error.message)
        verify(exactly = 0) { fines.insert(any()) }
    }

    @Test
    fun `lists the fines of a resident`() {
        val listed = listOf(
            FineResponse(UUID.randomUUID(), timestamp, BigDecimal("5.00"), sender.id, sender.name, receiver.id, receiver.name, false, "sent")
        )
        every { fines.findByResident(sender.id) } returns listed

        assertEquals(listed, service.findForResident(sender.id))
    }

    @Test
    fun `listing fines of an unknown resident is rejected`() {
        val unknown = UUID.randomUUID()
        every { users.findById(unknown) } returns null

        assertThrows<IllegalArgumentException> { service.findForResident(unknown) }
        verify(exactly = 0) { fines.findByResident(any()) }
    }

    @Test
    fun `marks an unpaid fine as paid`() {
        val fine = storedFine()
        every { fines.findById(fine.id) } returns fine
        every { fines.markPaid(fine.id) } returns true

        val result = service.markPaid(fine.id)

        assertTrue(result.isPaid)
        verify(exactly = 1) { fines.markPaid(fine.id) }
    }

    @Test
    fun `marking an already paid fine is a no-op`() {
        val fine = storedFine(isPaid = true)
        every { fines.findById(fine.id) } returns fine

        val result = service.markPaid(fine.id)

        assertTrue(result.isPaid)
        verify(exactly = 0) { fines.markPaid(any()) }
    }

    @Test
    fun `marking a missing fine throws not found`() {
        val id = UUID.randomUUID()
        every { fines.findById(id) } returns null

        assertThrows<FineNotFoundException> { service.markPaid(id) }
        verify(exactly = 0) { fines.markPaid(any()) }
    }

    private fun request(
        amount: Double = 5.5,
        sender: UUID = this.sender.id,
        receiver: UUID = this.receiver.id
    ) = FineRequest(amount = amount, sender = sender.toString(), receiver = receiver.toString(), timestamp = timestamp)

    private fun storedFine(isPaid: Boolean = false) = Fine(
        id = UUID.randomUUID(),
        timestamp = timestamp,
        amount = BigDecimal("5.00"),
        sender = sender.id,
        receiver = receiver.id,
        isPaid = isPaid
    )
}
