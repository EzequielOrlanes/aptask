package com.aptasks.config // <- Única mudança é o pacote

import com.aptasks.dto.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity <ApiResponse<Unit>> {
        val response = ApiResponse<Unit>(
            success = false,
            error = ex.message ?: "Dados de requisição inválidos.",
            timestamp = Instant.now().toString()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }
}