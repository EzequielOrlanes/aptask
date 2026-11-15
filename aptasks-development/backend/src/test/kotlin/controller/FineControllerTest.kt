package com.aptasks.controller

import com.aptasks.config.GlobalExceptionHandler
import com.aptasks.service.FineService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
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
    fun `returns success when a fine is accepted by the service`() {
        every { fineService.applyFine(any()) } just runs

        mockMvc.post("/api/fines") {
            contentType = MediaType.APPLICATION_JSON
            content = validFineRequest()
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.message") { value("Multa aplicada com sucesso! O bolso agradece e a casa também.") }
        }

        verify(exactly = 1) { fineService.applyFine(any()) }
    }

    @Test
    fun `returns bad request when fine data is invalid`() {
        every { fineService.applyFine(any()) } throws IllegalArgumentException("UUID do remetente (sender) é inválido.")

        mockMvc.post("/api/fines") {
            contentType = MediaType.APPLICATION_JSON
            content = validFineRequest()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.error") { value("UUID do remetente (sender) é inválido.") }
        }

        verify(exactly = 1) { fineService.applyFine(any()) }
    }

    private fun validFineRequest() = """
        {
          "sender": "${UUID.randomUUID()}",
          "receiver": "${UUID.randomUUID()}",
          "amount": 5.0,
          "timestamp": "2026-10-15T12:00:00"
        }
    """.trimIndent()
}
