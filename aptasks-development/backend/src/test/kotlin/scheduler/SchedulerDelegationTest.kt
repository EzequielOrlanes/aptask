package com.aptasks.scheduler

import com.aptasks.service.TaskRotationService
import com.aptasks.service.TaskService
import com.aptasks.service.WeeklyRotationResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.LocalDate

/** Os schedulers são casca fina: o teste garante que disparam o serviço certo. */
class SchedulerDelegationTest {

    @Test
    fun `overdue scheduler asks TaskService to evaluate overdue tasks`() {
        val taskService = mockk<TaskService>()
        every { taskService.evaluateOverdueTasks(any()) } returns 2

        OverdueTaskScheduler(taskService).evaluateOverdueTasks()

        verify(exactly = 1) { taskService.evaluateOverdueTasks(any()) }
    }

    @Test
    fun `rotation scheduler asks TaskRotationService to rotate the current week`() {
        val rotation = mockk<TaskRotationService>()
        every { rotation.rotateCurrentWeek(any()) } returns
            WeeklyRotationResult(LocalDate.of(2026, 10, 12), false, 0, 0, emptyMap())

        WeeklyTaskRotationScheduler(rotation).rotateTasks()

        verify(exactly = 1) { rotation.rotateCurrentWeek(any()) }
    }
}
