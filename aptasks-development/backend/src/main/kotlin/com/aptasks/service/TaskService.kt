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

//    fun isTasksLate(id: UUID): Boolean{
//        // quero que avlie se uma tarefa já passou do tempo de ser feita e se sim gera uma multa usando o fineService.
//        //applyFine()
//
//
//    }


    fun getDashboardData(): DashboardResponse {
        val allTasks = taskRepository.findAll()
        val total = allTasks.size
        var completed = allTasks.count { it.isCompleted }
        var pending = total - completed
        val recent = allTasks.take(5)

        return DashboardResponse(total, completed, pending, recent)
    }

    fun toggleStatus(id: UUID) { // Mantido como Long para alinhar com o Repository
        var task = taskRepository.findById(id).orElseThrow {
            IllegalArgumentException("Tarefa não encontrada com o ID: $id")
        }
        task.isCompleted = !task.isCompleted
        taskRepository.save(task)
    }
}