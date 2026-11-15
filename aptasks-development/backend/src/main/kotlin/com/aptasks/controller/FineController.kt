package com.aptasks.controller

import com.aptasks.dto.ApiResponse
import com.aptasks.dto.fine.FineRequest
import com.aptasks.dto.fine.FineResponse
import com.aptasks.model.Fine
import com.aptasks.service.FineService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/fines")
class FineController(private val fineService: FineService) {

    /** Registra uma multa manual entre dois moradores. Erros de validação viram 400 pelo GlobalExceptionHandler. */
    @PostMapping
    fun createFine(@RequestBody dto: FineRequest): ResponseEntity<ApiResponse<Fine>> {
        val fine = fineService.applyFine(dto)
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Multa aplicada com sucesso! O bolso agradece e a casa também.",
                data = fine,
                timestamp = Instant.now().toString()
            )
        )
    }

    /** Multas enviadas e recebidas pelo morador informado em `userId`. */
    @GetMapping
    fun listFines(@RequestParam userId: String): ResponseEntity<ApiResponse<List<FineResponse>>> {
        val id = parseUuid(userId, "O identificador do morador é inválido.")
        val fines = fineService.findForResident(id)
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                data = fines,
                count = fines.size,
                timestamp = Instant.now().toString()
            )
        )
    }

    /** Marca uma multa como paga. */
    @PatchMapping("/{id}/paid")
    fun markPaid(@PathVariable id: String): ResponseEntity<ApiResponse<Fine>> {
        val fine = fineService.markPaid(parseUuid(id, "O identificador da multa é inválido."))
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Multa marcada como paga.",
                data = fine,
                timestamp = Instant.now().toString()
            )
        )
    }

    /** Todas as multas, para o administrador. */
    @GetMapping("/admin")
    fun listAllFines(): ResponseEntity<ApiResponse<List<FineResponse>>> {
        val fines = fineService.findAll()
        return ResponseEntity.ok(ApiResponse(success = true, data = fines, count = fines.size, timestamp = Instant.now().toString()))
    }

    /** Remove uma multa (somente administrador, rota protegida pelo AdminAuthInterceptor). */
    @DeleteMapping("/{id}")
    fun deleteFine(@PathVariable id: String): ResponseEntity<ApiResponse<Unit>> {
        fineService.deleteFine(parseUuid(id, "O identificador da multa é inválido."))
        return ResponseEntity.ok(ApiResponse(success = true, message = "Multa removida.", timestamp = Instant.now().toString()))
    }

    private fun parseUuid(value: String, message: String): UUID =
        runCatching { UUID.fromString(value) }.getOrNull() ?: throw IllegalArgumentException(message)
}
