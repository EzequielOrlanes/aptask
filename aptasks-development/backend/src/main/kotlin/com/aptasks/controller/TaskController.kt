package com.aptasks.controller

import com.aptasks.dto.CreateTaskRequest
import com.aptasks.service.TaskService
import com.aptasks.dto.ApiResponse
import com.aptasks.dto.UpdateTaskRequest
import com.aptasks.dto.TaskDecisionRequest
import com.aptasks.dto.task.WeeklyTaskResponse
import com.aptasks.dto.task.TaskRotationHistoryResponse
import com.aptasks.model.Task
import com.aptasks.service.TaskRotationService
import com.aptasks.service.WeeklyBoardService
import com.aptasks.dto.task.WeeklyBoardResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/tasks")
class TaskController(
    private val taskService: TaskService,
    private val taskRotationService: TaskRotationService,
    private val weeklyBoardService: WeeklyBoardService
) {
    @GetMapping("/board")
    fun readWeeklyBoard(): ResponseEntity<ApiResponse<WeeklyBoardResponse>> {
        // Também recupera a semana se o scheduler esteve suspenso.
        taskRotationService.rotateCurrentWeek()
        val board = weeklyBoardService.getBoard()
        return ResponseEntity.ok(ApiResponse(success = true, message = "Quadro semanal carregado.", data = board))
    }

    @GetMapping("/weekly")
    fun readWeeklyTasks(): ResponseEntity<ApiResponse<List<WeeklyTaskResponse>>> {
        // Também recupera a semana se o scheduler esteve suspenso.
        taskRotationService.rotateCurrentWeek()
        val tasks = taskService.getWeeklyTasks()
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Distribuição semanal carregada.", data = tasks, count = tasks.size)
        )
    }

    @GetMapping("/history")
    fun readRotationHistory(
        @RequestParam(required = false) userId: UUID?
    ): ResponseEntity<ApiResponse<List<TaskRotationHistoryResponse>>> {
        val history = taskRotationService.getHistory(userId)
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Histórico de distribuição carregado.",
                data = history,
                count = history.size
            )
        )
    }

    @PatchMapping("/{id}/decision")
    fun setTaskDecision(
        @PathVariable id: UUID,
        @RequestBody request: TaskDecisionRequest
    ): ResponseEntity<ApiResponse<WeeklyTaskResponse>> = ResponseEntity.ok(
        ApiResponse(success = true, message = "Decisão da tarefa atualizada.", data = taskService.setCompletion(id, request.done))
    )

    @PostMapping("/create")
    fun createTask(@RequestBody request: CreateTaskRequest): ResponseEntity<ApiResponse<Task>> {
        val task = taskService.createTask(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse(success = true, message = "Tarefa criada com sucesso.", data = task)
        )
    }

    @GetMapping(value = ["", "/read"])
    fun readTasks(): ResponseEntity<ApiResponse<List<Task>>> {
        val tasks = taskService.getAllTasks()
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Tarefas listadas com sucesso.", data = tasks, count = tasks.size)
        )
    }

    @GetMapping("/{id}")
    fun readTask(@PathVariable id: UUID): ResponseEntity<ApiResponse<Task>> =
        ResponseEntity.ok(ApiResponse(success = true, data = taskService.getTask(id)))

    @PutMapping("/{id}")
    fun updateTask(
        @PathVariable id: UUID,
        @RequestBody request: UpdateTaskRequest
    ): ResponseEntity<ApiResponse<Task>> = ResponseEntity.ok(
        ApiResponse(success = true, message = "Tarefa atualizada com sucesso.", data = taskService.updateTask(id, request))
    )

    @DeleteMapping("/{id}")
    fun deleteTask(@PathVariable id: UUID): ResponseEntity<ApiResponse<Unit>> {
        taskService.deleteTask(id)
        return ResponseEntity.ok(ApiResponse(success = true, message = "Tarefa removida com sucesso."))
    }

    //Rota para o usuário marcar/desmarcar como feito (Toggle)
    @PatchMapping("/{id}/toggle")
    fun updateStatus(@PathVariable id: UUID): ResponseEntity<ApiResponse<String>> {
        taskService.toggleStatus(id)
        return ResponseEntity.ok(ApiResponse<String>(true, "Status da tarefa atualizado com sucesso"))
    }

}
