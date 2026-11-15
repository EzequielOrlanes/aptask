package com.aptasks.config // <- Única mudança é o pacote

import com.aptasks.dto.ApiResponse
import com.aptasks.exception.FineNotFoundException
import com.aptasks.security.AdminAuthUnavailableException
import com.aptasks.security.InvalidAdminCredentialsException
import com.aptasks.exception.TaskNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException::class)
    fun handleTaskNotFoundException(ex: TaskNotFoundException): ResponseEntity<ApiResponse<Unit>> {
        val response = ApiResponse<Unit>(
            success = false,
            error = ex.message ?: "Tarefa não encontrada.",
            timestamp = Instant.now().toString()
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    @ExceptionHandler(FineNotFoundException::class)
    fun handleFineNotFoundException(ex: FineNotFoundException): ResponseEntity<ApiResponse<Unit>> {
        val response = ApiResponse<Unit>(
            success = false,
            error = ex.message ?: "Multa não encontrada.",
            timestamp = Instant.now().toString()
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    @ExceptionHandler(InvalidAdminCredentialsException::class)
    fun handleInvalidAdmin(ex: InvalidAdminCredentialsException): ResponseEntity<ApiResponse<Unit>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse(success = false, error = ex.message, timestamp = Instant.now().toString()))

    @ExceptionHandler(AdminAuthUnavailableException::class)
    fun handleAdminUnavailable(ex: AdminAuthUnavailableException): ResponseEntity<ApiResponse<Unit>> =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse(success = false, error = ex.message, timestamp = Instant.now().toString()))

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
