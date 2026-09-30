package com.aptasks.controller

import com.aptasks.dto.ApiResponse
import com.aptasks.service.FineService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.aptasks.dto.fine.FineRequest


@RestController
@RequestMapping("/api/fines")
class FineController(private val fineService: FineService) {

    @PostMapping
    fun createFine(@RequestBody dto: FineRequest): ResponseEntity<ApiResponse<String>> {
        fineService.applyFine(dto)
        return ResponseEntity.ok(ApiResponse<String>(true, "Multa aplicada com sucesso! O bolso agradece e a casa também."))
    }
}