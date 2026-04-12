package com.aptasks.controller

import com.aptasks.dto.LoginRequest
import com.aptasks.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {
    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<Map<String, Any>> {
        return try {
            // Validação
            if (loginRequest.email.isBlank()) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(
                        mapOf(
                            "success" to false,
                            "message" to "Email é obrigatório"
                        )
                    )
            }
            if (loginRequest.password.isBlank()) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(
                        mapOf(
                            "success" to false,
                            "message" to "Senha é obrigatória"
                        )
                    )
            }
            
            val loginResponse = authService.login(loginRequest)
            
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "Login realizado com sucesso",
                    "data" to loginResponse
                )
            )
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Erro desconhecido"
            val status = if (errorMessage.contains("autenticação") || errorMessage.contains("Invalid")) {
                HttpStatus.UNAUTHORIZED
            } else {
                HttpStatus.INTERNAL_SERVER_ERROR
            }
            
            ResponseEntity
                .status(status)
                .body(
                    mapOf(
                        "success" to false,
                        "message" to "Falha no login",
                        "error" to errorMessage
                    )
                )
        }
    }
}
