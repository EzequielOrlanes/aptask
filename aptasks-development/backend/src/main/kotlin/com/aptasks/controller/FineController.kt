package com.aptasks.controller

import com.aptasks.dto.ApiResponse
import com.aptasks.dto.fine.FineRequest
import com.aptasks.service.FineService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/fines")
class FineController(private val fineService: FineService) {

    @PostMapping
    fun createFine(@RequestBody dto: FineRequest): ResponseEntity < ApiResponse < String > > {
        return try {
            fineService.applyFine(dto)
            ResponseEntity.ok(
                ApiResponse(
                    success = true,
                    message = "Multa aplicada com sucesso! O bolso agradece e a casa também.",
                    timestamp = Instant.now().toString()
                )
            )
        } catch (ex: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                    ApiResponse(
                        success = false,
                        error = ex.message ?: "Dados de requisição inválidos.",
                        timestamp = Instant.now().toString()
                    )
                )
        }
    }
}