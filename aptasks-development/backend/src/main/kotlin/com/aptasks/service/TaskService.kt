package com.aptasks.service

import com.aptasks.dto.CreateTaskRequest
import com.aptasks.model.Task
import com.aptasks.repository.TaskRepository
import org.springframework.stereotype.Service
import java.util.UUID
import com.aptasks.dto.DashboardResponse


@Service
class TaskService(
    private val taskRepository: TaskRepository
){
    fun createTask(request: CreateTaskRequest): Task {
        val newTask = Task(
            id = UUID.randomUUID(),
            description = request.description,
            inTime = true,
            userId = request.responsible_user.toString(),
            isCompleted = false
        )
        return taskRepository.save(newTask)
    }


    fun getDashboardData(): DashboardResponse {
        val allTasks = taskRepository.findAll()
        val total = allTasks.size
        val completed = allTasks.count { it.isCompleted }
        val pending = total - completed
        val recent = allTasks.take(5)

        return DashboardResponse(total, completed, pending, recent)
    }

    fun toggleStatus(id: UUID) { // Mantido como Long para alinhar com o Repository
        val task = taskRepository.findById(id).orElseThrow {
            IllegalArgumentException("Tarefa não encontrada com o ID: $id")
        }
        task.isCompleted = !task.isCompleted
        taskRepository.save(task)
    }
}