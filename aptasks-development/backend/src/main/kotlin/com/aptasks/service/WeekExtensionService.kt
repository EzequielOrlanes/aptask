package com.aptasks.service

import com.aptasks.repository.TaskRepository
import com.aptasks.repository.TaskRotationHistoryRepository
import com.aptasks.repository.TaskRotationRepository
import com.aptasks.repository.TaskWeekExtensionRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val extensionLogger = KotlinLogging.logger {}

data class WeekExtensionResult(
    val weekStart: LocalDate,
    val extendedUntil: Instant,
    val taskCount: Int,
    val finesCancelled: Int,
    val finesKeptPaid: Int
)

/** Prorroga o prazo da semana em vigor, mesmo depois de o prazo original ter vencido. */
@Service
class WeekExtensionService(
    private val taskRepository: TaskRepository,
    private val fineService: FineService,
    private val taskRotationRepository: TaskRotationRepository,
    private val extensionRepository: TaskWeekExtensionRepository,
    private val historyRepository: TaskRotationHistoryRepository,
    private val rotationService: TaskRotationService
) {
    /**
     * Define [until] como novo prazo de todas as tarefas da última semana rotacionada e cancela as
     * multas automáticas dela. A rotação seguinte só acontece depois desse prazo
     * (ver [TaskRotationService.activeExtension]).
     */
    @Transactional
    fun extendCurrentWeek(until: Instant, now: Instant = Instant.now()): WeekExtensionResult {
        require(until.isAfter(now)) { "O novo prazo precisa estar no futuro." }
        val weekStart = taskRotationRepository.latestWeek()
            ?: throw IllegalArgumentException("Nenhuma semana foi iniciada ainda, não há prazo para prorrogar.")

        val tasks = taskRepository.findAll()
        var cancelled = 0
        var keptPaid = 0

        tasks.forEach { task ->
            val appliedAt = task.fineAppliedAt
            var keepFineMark = false
            if (appliedAt != null) {
                val receiver = runCatching { UUID.fromString(task.userId) }.getOrNull()
                if (receiver != null) {
                    val result = fineService.cancelAutomaticFines(receiver, appliedAt)
                    cancelled += result.cancelled
                    keptPaid += result.keptPaid
                    // Uma multa já paga continua valendo: a tarefa não pode ser multada de novo.
                    keepFineMark = result.keptPaid > 0
                }
                if (!keepFineMark) taskRepository.clearFineApplied(task.id)
            }
            taskRepository.postponeDeadline(task.id, until)
        }

        extensionRepository.upsert(weekStart, until, now)
        extensionLogger.info {
            "Semana $weekStart prorrogada até $until: ${tasks.size} tarefas, $cancelled multas canceladas, $keptPaid pagas mantidas."
        }
        return WeekExtensionResult(weekStart, until, tasks.size, cancelled, keptPaid)
    }

    /**
     * Reabre a semana anterior à que está em vigor: ela volta a ser a atual até [until] e a semana em
     * vigor passa a ser a próxima. As tarefas voltam a quem as tinha, com o estado de então; a rotação
     * da semana em vigor é desfeita para acontecer de novo depois do novo prazo; as multas aplicadas
     * quando a semana venceu são canceladas; e o resultado dela é apagado para ser gravado outra vez.
     * Tarefas apagadas depois daquela semana não voltam.
     */
    @Transactional
    fun reopenPreviousWeek(until: Instant, now: Instant = Instant.now()): WeekExtensionResult {
        require(until.isAfter(now)) { "O novo prazo precisa estar no futuro." }
        val currentWeek = rotationService.currentWeekStart(now)
        val previousWeek = currentWeek.minusWeeks(1)
        val rows = historyRepository.findOutcomesByWeek(previousWeek)
        require(rows.isNotEmpty()) { "Não há registro da semana anterior para reabrir." }

        // As multas da semana saem quando ela venceu, antes de a rotação seguinte rodar.
        val finesFrom = rotationService.weekDeadline(previousWeek)
        val finesTo = taskRotationRepository.executedAt(currentWeek) ?: now
        val paidKept = mutableSetOf<UUID>()
        var cancelled = 0
        var keptPaid = 0
        rows.filter { it.fineApplied != false }.map { it.userId }.distinct().forEach { receiver ->
            val result = fineService.cancelFinesBetween(receiver, finesFrom, finesTo)
            cancelled += result.cancelled
            keptPaid += result.keptPaid
            if (result.keptPaid > 0) paidKept += receiver
        }

        val liveTasks = taskRepository.findAll().associateBy { it.id }
        val restored = mutableSetOf<UUID>()
        rows.forEach { row ->
            val taskId = row.taskId ?: return@forEach
            if (taskId !in liveTasks) return@forEach
            taskRepository.restoreForReopen(
                taskId = taskId,
                residentId = row.userId,
                dueAt = until,
                isCompleted = row.isCompleted ?: false,
                decisionAt = row.decisionAt,
                // Uma multa já paga continua valendo: a tarefa não pode ser multada de novo.
                fineAppliedAt = if (row.userId in paidKept) finesFrom else null
            )
            restored += taskId
        }
        // Tarefas criadas depois daquela semana ficam como estão, só com o novo prazo.
        liveTasks.keys.filterNot { it in restored }.forEach { taskId ->
            taskRepository.clearFineApplied(taskId)
            taskRepository.postponeDeadline(taskId, until)
        }

        historyRepository.clearOutcomes(previousWeek)
        historyRepository.deleteWeeksAfter(previousWeek)
        taskRotationRepository.deleteWeeksAfter(previousWeek)
        extensionRepository.deleteWeeksAfter(previousWeek)
        extensionRepository.upsert(previousWeek, until, now)

        extensionLogger.info {
            "Semana $previousWeek reaberta até $until: ${restored.size} tarefas restauradas, " +
                "$cancelled multas canceladas, $keptPaid pagas mantidas."
        }
        return WeekExtensionResult(previousWeek, until, liveTasks.size, cancelled, keptPaid)
    }
}
