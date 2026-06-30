package com.aptasks.controller

import com.aptasks.dto.CreateTaskRequest
import com.aptasks.service.TaskService
import com.aptasks.dto.ApiResponse
// Se o DashboardResponse estiver em outro pacote (ex: com.aptasks.dto.task), mude o import abaixo:
import com.aptasks.dto.DashboardResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/tasks")
class TaskController(
    private val taskService: TaskService
) {
    // 1. Carrega todos os dados da Home unificados
    @GetMapping("/dashboard")
    fun getDashboardData(): ResponseEntity<DashboardResponse> {
        val data = taskService.getDashboardData()
        return ResponseEntity.ok(data)
    }

    // 2. Rota para o usuário marcar/desmarcar como feito (Toggle)
    @PatchMapping("/{id}/toggle")
    fun toggleTaskStatus(@PathVariable id: UUID): ResponseEntity<ApiResponse<String>> { // Adicionado <String>
        taskService.toggleStatus(id)
        // Adicionado <String> também na instância do ApiResponse
        return ResponseEntity.ok(ApiResponse<String>(true, "Status da tarefa atualizado com sucesso"))
    }

    @PostMapping("/create")
    fun create_task(@RequestBody createtaskRequest: CreateTaskRequest): ResponseEntity<Map<String, Any>> {
        return try {
            val taskResponse = taskService.createTask(createtaskRequest)
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "New task is created.",
                    "data" to taskResponse
                )
            )
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknown error"
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
                        "message" to "Failed during create a new task.",
                        "error" to errorMessage
                    )
                )
        }
    }
}