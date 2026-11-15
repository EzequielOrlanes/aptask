package com.aptasks.scheduler

import com.aptasks.service.TaskService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}
@Component
class OverdueTaskScheduler(private val taskService: TaskService) {
    /** Avalia tarefas vencidas uma vez por hora. */
    @Scheduled(cron = "\${tasks.overdue.cron:0 0 * * * *}")
    fun evaluateOverdueTasks() {
        val postponed = taskService.evaluateOverdueTasks()
        if (postponed > 0) {
            logger.info { "$postponed tarefa(s) tiveram o prazo prorrogado por ausência do responsável." }
        }
    }
}
