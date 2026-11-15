package com.aptasks.controller

import com.aptasks.dto.ApiResponse
import com.aptasks.dto.ResidentPresenceRequest
import com.aptasks.dto.ResidentResponse
import com.aptasks.model.User
import com.aptasks.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/residents")
class ResidentController(private val userRepository: UserRepository) {
    @GetMapping
    fun getResidents(): ResponseEntity<ApiResponse<List<ResidentResponse>>> {
        val residents = userRepository.findAll().map { it.toResponse() }
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Moradores listados com sucesso.", data = residents, count = residents.size)
        )
    }

    @PatchMapping("/{id}/presence")
    fun updatePresence(
        @PathVariable id: UUID,
        @RequestBody request: ResidentPresenceRequest
    ): ResponseEntity<ApiResponse<ResidentResponse>> {
        val resident = userRepository.updatePresence(id, request.isAtHome)
            ?: throw IllegalArgumentException("Morador não encontrado.")
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Presença atualizada.", data = resident.toResponse())
        )
    }

    private fun User.toResponse() = ResidentResponse(id, name, isAtHome)
}
