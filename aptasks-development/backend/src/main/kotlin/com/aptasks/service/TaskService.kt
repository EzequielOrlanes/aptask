package com.aptasks.service

import com.aptasks.dto.CreateTaskRequest
import com.aptasks.model.Task
import com.aptasks.repository.TaskRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TaskService(
    private val taskRepository: TaskRepository
){
    fun createTask(request: CreateTaskRequest): Task {

        val newTask = Task(
            id = UUID.randomUUID(),
            description = request.description,
            inTime = true,
            userId = request.responsible_user.toString()
        )
        return taskRepository.save(newTask)
    }

}