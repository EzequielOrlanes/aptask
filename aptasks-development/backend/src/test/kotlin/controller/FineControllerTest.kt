package com.aptasks.controller

import com.aptasks.config.GlobalExceptionHandler
import com.aptasks.dto.fine.FineResponse
import com.aptasks.exception.FineNotFoundException
import com.aptasks.model.Fine
import com.aptasks.service.FineService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class FineControllerTest {
    private lateinit var mockMvc: MockMvc
    private val fineService: FineService = mockk()

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(FineController(fineService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `creates a fine and returns it in data`() {
        val fine = storedFine()
        every { fineService.applyFine(any()) } returns fine

        mockMvc.post("/fines") {
            contentType = MediaType.APPLICATION_JSON
            content = validFineRequest()
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.message") { value("Multa aplicada com sucesso! O bolso agradece e a casa também.") }
            jsonPath("$.data.id") { value(fine.id.toString()) }
        }

        verify(exactly = 1) { fineService.applyFine(any()) }
    }

    @Test
    fun `returns bad request when fine data is invalid`() {
        every { fineService.applyFine(any()) } throws IllegalArgumentException("UUID do remetente (sender) é inválido.")

        mockMvc.post("/fines") {
            contentType = MediaType.APPLICATION_JSON
            content = validFineRequest()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.error") { value("UUID do remetente (sender) é inválido.") }
        }

        verify(exactly = 1) { fineService.applyFine(any()) }
    }

    @Test
    fun `lists the fines of a resident with a count`() {
        val userId = UUID.randomUUID()
        val fine = FineResponse(
            id = UUID.randomUUID(),
            timestamp = LocalDateTime.of(2026, 10, 15, 12, 0),
            amount = BigDecimal("5.00"),
            senderId = UUID.randomUUID(),
            senderName = "Casa",
            receiverId = userId,
            receiverName = "Bia",
            isPaid = false,
            direction = "received"
        )
        every { fineService.findForResident(userId) } returns listOf(fine)

        mockMvc.get("/fines") { param("userId", userId.toString()) }.andExpect {
            status { isOk() }
            jsonPath("$.count") { value(1) }
            jsonPath("$.data[0].direction") { value("received") }
            jsonPath("$.data[0].receiverName") { value("Bia") }
        }
    }

    @Test
    fun `listing with a malformed userId is a bad request`() {
        mockMvc.get("/fines") { param("userId", "nao-uuid") }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("O identificador do morador é inválido.") }
        }
    }

    @Test
    fun `marks a fine as paid`() {
        val fine = storedFine(isPaid = true)
        every { fineService.markPaid(fine.id) } returns fine

        mockMvc.patch("/fines/${fine.id}/paid").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.isPaid") { value(true) }
        }
    }

    @Test
    fun `marking an unknown fine returns not found`() {
        val id = UUID.randomUUID()
        every { fineService.markPaid(id) } throws FineNotFoundException(id)

        mockMvc.patch("/fines/$id/paid").andExpect {
            status { isNotFound() }
            jsonPath("$.success") { value(false) }
        }
    }

    @Test
    fun `marking a fine with a malformed id is a bad request`() {
        mockMvc.patch("/fines/nao-uuid/paid").andExpect {
            status { isBadRequest() }
        }
        verify(exactly = 0) { fineService.markPaid(any()) }
    }

    private fun storedFine(isPaid: Boolean = false) = Fine(
        timestamp = LocalDateTime.of(2026, 10, 15, 12, 0),
        amount = BigDecimal("5.00"),
        sender = UUID.randomUUID(),
        receiver = UUID.randomUUID(),
        isPaid = isPaid
    )

    private fun validFineRequest() = """
        {
          "sender": "${UUID.randomUUID()}",
          "receiver": "${UUID.randomUUID()}",
          "amount": 5.0,
          "timestamp": "2026-10-15T12:00:00"
        }
    """.trimIndent()
}
