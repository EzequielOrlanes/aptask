package com.aptasks.controller

import com.aptasks.dto.ApiResponse
import com.aptasks.dto.fine.FineRequest
import com.aptasks.service.FineService
import com.fasterxml.jackson.databind.ObjectMapper
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

class FineControllerTest {

    private lateinit var mockMvc: MockMvc
    private val fineService: FineService = mockk()
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        val fineController = FineController(fineService)
        mockMvc = MockMvcBuilders.standaloneSetup(fineController).build()
    }

    @Test
    fun `should create fine successfully and return 200 OK`() {
        // Arrange
        val requestDto = FineRequest(
            senderId = 1L,
            receiverId = 2L,
            taskId = 10L,
            amount = 5.0
        )

        every { fineService.applyFine(any()) } just runs

        // Act & Assert
        mockMvc.post("/api/fines") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(requestDto)
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.message") { value("Multa aplicada com sucesso! O bolso agradece e a casa também.") }
        }

        // Verify
        verify(exactly = 1) { fineService.applyFine(any()) }
    }
}