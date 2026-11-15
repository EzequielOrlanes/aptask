package com.aptasks.controller

import com.aptasks.dto.ApiResponse
import com.aptasks.security.AdminAuthService
import com.aptasks.security.AdminSession
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

data class AdminLoginRequest(val email: String = "")

@RestController
@RequestMapping("/auth/admin")
class AdminAuthController(private val adminAuth: AdminAuthService) {

    @PostMapping("/login")
    fun login(@RequestBody request: AdminLoginRequest): ResponseEntity<ApiResponse<AdminSession>> {
        val session = adminAuth.login(request.email)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Administrador autenticado.", data = session, timestamp = Instant.now().toString())
        )
    }
}
