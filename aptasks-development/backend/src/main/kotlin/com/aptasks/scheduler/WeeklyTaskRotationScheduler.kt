package com.aptasks.scheduler

import com.aptasks.service.TaskRotationService
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val rotationLogger = KotlinLogging.logger {}

@Component
class WeeklyTaskRotationScheduler(private val taskRotationService: TaskRotationService) {
    /** Executa diariamente; o serviço só altera tarefas uma vez por semana. */
    @Scheduled(cron = "\${tasks.rotation.cron:0 10 0 * * *}", zone = "America/Sao_Paulo")
    fun rotateTasks() {
        val result = taskRotationService.rotateCurrentWeek()
        if (result.rotated) {
            rotationLogger.info {
                "Rotação da semana ${result.weekStart}: ${result.taskCount} tarefas, " +
                    "${result.memberCount} moradores; distribuição ${result.assignmentsPerMember}"
            }
        }
    }
}
