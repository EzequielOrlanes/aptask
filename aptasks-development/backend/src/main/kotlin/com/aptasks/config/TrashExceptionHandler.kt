package com.aptasks.config

import com.aptasks.controller.TrashController
import com.aptasks.dto.ApiResponse
import com.aptasks.exception.TrashConflictException
import com.aptasks.exception.TrashForbiddenException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice(assignableTypes = [TrashController::class])
class TrashExceptionHandler {

    @ExceptionHandler(TrashConflictException::class)
    fun handleTrashConflictException(ex: TrashConflictException): ResponseEntity<ApiResponse<Unit>> {
        val response = ApiResponse<Unit>(
            success = false,
            error = ex.message ?: "Conflito no estado da lixeira.",
            timestamp = Instant.now().toString()
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }

    @ExceptionHandler(TrashForbiddenException::class)
    fun handleTrashForbiddenException(ex: TrashForbiddenException): ResponseEntity<ApiResponse<Unit>> {
        val response = ApiResponse<Unit>(
            success = false,
            error = ex.message ?: "Ação não permitida para este morador.",
            timestamp = Instant.now().toString()
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response)
    }
}
