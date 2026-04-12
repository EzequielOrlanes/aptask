package com.aptasks.controller

import com.aptasks.dto.CreateTaskRequest
import com.aptasks.service.TaskService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/tasks")
class TaskController(
    private val taskService: TaskService
){

    @PostMapping("/create")
    fun create_task(@RequestBody createtaskRequest: CreateTaskRequest): ResponseEntity <Map<String, Any>>{
        return try{
        //the user is admin?
      val taskResponse = taskService.createTask(createtaskRequest)
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "New task is created.",
                    "data" to taskResponse
                )
            )
        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknow error"
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