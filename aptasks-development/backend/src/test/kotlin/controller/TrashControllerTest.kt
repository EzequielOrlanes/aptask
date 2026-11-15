package com.aptasks.controller

import com.aptasks.config.GlobalExceptionHandler
import com.aptasks.config.TrashExceptionHandler
import com.aptasks.dto.ResidentResponse
import com.aptasks.dto.trash.TrashStatusResponse
import com.aptasks.exception.TrashConflictException
import com.aptasks.exception.TrashForbiddenException
import com.aptasks.service.TrashService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.util.UUID

class TrashControllerTest {
    private lateinit var mockMvc: MockMvc
    private val trashService: TrashService = mockk()

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(TrashController(trashService))
            .setControllerAdvice(GlobalExceptionHandler(), TrashExceptionHandler())
            .build()
    }

    @Test
    fun `GET today returns the status with fillLevel and isFull`() {
        every { trashService.getTodayStatus() } returns fullStatus()

        mockMvc.get("/trash/today").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.message") { value("Status do lixo carregado.") }
            jsonPath("$.data.fillLevel") { value("FULL") }
            jsonPath("$.data.isFull") { value(true) }
            jsonPath("$.data.responsible.name") { value("Ana") }
        }
    }

    @Test
    fun `PATCH responsible returns success`() {
        every { trashService.assignResponsible(any(), any()) } returns fullStatus()

        mockMvc.patch("/trash/responsible") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"residentId": "${UUID.randomUUID()}"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.message") { value("Responsável pelo lixo atualizado.") }
        }
    }

    @Test
    fun `PATCH responsible with an unknown resident returns bad request`() {
        every { trashService.assignResponsible(any(), any()) } throws
            IllegalArgumentException("Morador não encontrado.")

        mockMvc.patch("/trash/responsible") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"residentId": "${UUID.randomUUID()}"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.error") { value("Morador não encontrado.") }
        }
    }

    @Test
    fun `POST half returns success with HALF status`() {
        every { trashService.reportHalf(any(), any()) } returns halfStatus()

        mockMvc.post("/trash/half") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"reportedByResidentId": "${UUID.randomUUID()}"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.message") { value("Aviso de lixo meio cheio registrado.") }
            jsonPath("$.data.fillLevel") { value("HALF") }
            jsonPath("$.data.isFull") { value(false) }
        }
    }

    @Test
    fun `POST half when already half or full returns 200 with the current status`() {
        every { trashService.reportHalf(any(), any()) } returns fullStatus()

        mockMvc.post("/trash/half") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"reportedByResidentId": "${UUID.randomUUID()}"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.fillLevel") { value("FULL") }
        }
    }

    @Test
    fun `POST full returns success`() {
        every { trashService.reportFull(any(), any()) } returns fullStatus()

        mockMvc.post("/trash/full") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"reportedByResidentId": "${UUID.randomUUID()}"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.message") { value("Aviso de lixo cheio registrado.") }
            jsonPath("$.data.isFull") { value(true) }
        }
    }

    @Test
    fun `POST full when already full returns 200 with the current status`() {
        every { trashService.reportFull(any(), any()) } returns TrashStatusResponse(
            responsible = null, fillLevel = "FULL", isFull = true, reportedAt = Instant.now(), reportedBy = null
        )

        mockMvc.post("/trash/full") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"reportedByResidentId": "${UUID.randomUUID()}"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.data.isFull") { value(true) }
        }
    }

    @Test
    fun `POST empty when already empty returns conflict`() {
        every { trashService.markEmptied(any(), any()) } throws
            TrashConflictException("O lixo já está vazio.")

        mockMvc.post("/trash/empty") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"updatedByResidentId": "${UUID.randomUUID()}"}"""
        }.andExpect {
            status { isConflict() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.error") { value("O lixo já está vazio.") }
        }
    }

    @Test
    fun `POST empty by a resident who is not responsible returns 403`() {
        every { trashService.markEmptied(any(), any()) } throws
            TrashForbiddenException("Só o responsável pelo lixo pode marcar como esvaziado.")

        mockMvc.post("/trash/empty") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"updatedByResidentId": "${UUID.randomUUID()}"}"""
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.error") { value("Só o responsável pelo lixo pode marcar como esvaziado.") }
        }
    }

    @Test
    fun `POST empty without updatedByResidentId returns bad request`() {
        every { trashService.markEmptied(null, any()) } throws
            IllegalArgumentException("O identificador do morador é obrigatório.")

        mockMvc.post("/trash/empty") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.error") { value("O identificador do morador é obrigatório.") }
        }
    }

    private fun fullStatus() = TrashStatusResponse(
        responsible = ResidentResponse(UUID.randomUUID(), "Ana", true),
        fillLevel = "FULL",
        isFull = true,
        reportedAt = Instant.parse("2026-11-15T10:00:00Z"),
        reportedBy = ResidentResponse(UUID.randomUUID(), "Bruno", false)
    )

    private fun halfStatus() = TrashStatusResponse(
        responsible = ResidentResponse(UUID.randomUUID(), "Ana", true),
        fillLevel = "HALF",
        isFull = false,
        reportedAt = Instant.parse("2026-11-15T10:00:00Z"),
        reportedBy = ResidentResponse(UUID.randomUUID(), "Bruno", false)
    )
}
