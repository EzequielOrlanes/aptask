//package com.aptasks.service
//
//import com.aptasks.dto.CreateTaskRequest
//import com.aptasks.dto.DashboardResponse
//import com.aptasks.model.Task
//import com.aptasks.repository.TaskRepository
//import io.mockk.every
//import io.mockk.impl.annotations.InjectMockKs
//import io.mockk.impl.annotations.MockK
//import io.mockk.junit5.MockKExtension
//import io.mockk.slot
//import io.mockk.verify
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Assertions.assertFalse
//import org.junit.jupiter.api.Assertions.assertNotNull
//import org.junit.jupiter.api.Assertions.assertTrue
//import org.junit.jupiter.api.Nested
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.assertThrows
//import org.junit.jupiter.api.extension.ExtendWith
//import java.util.Optional
//import java.util.UUID
//
//@ExtendWith(MockKExtension::class)
//class TaskServiceTest {
//
//    @MockK
//    private lateinit var taskRepository: TaskRepository
//    @InjectMockKs
//    private lateinit var taskService: TaskService
//    @Nested
//    inner class CreateTask {
//
//        @Test
//        fun `deve criar e salvar uma nova tarefa com sucesso`() {
//            // Arrange
//            val userId = UUID.randomUUID()
//            val request = CreateTaskRequest(
//                description = "Lavar a geladeira por fora e por dentro.",
//                responsible_user = userId
//            )
//
//            val taskSlot = slot<Task>()
//            every { taskRepository.save(capture(taskSlot)) } answers { taskSlot.captured }
//
//            // Act
//            val result = taskService.createTask(request)
//
//            // Assert
//            assertNotNull(result.id)
//            assertEquals("Estudar testes com MockK", result.description)
//            assertEquals(userId.toString(), result.userId)
//            assertTrue(result.inTime)
//            assertFalse(result.isCompleted)
//
//            verify(exactly = 1) { taskRepository.save(any()) }
//        }
//    }
//
//    @Nested
//    inner class GetDashboardData {
//
//        @Test
//        fun `deve calcular os dados do dashboard corretamente`() {
//            // Arrange
//            val tasks = listOf(
//                Task(id = UUID.randomUUID(), description = "T1", inTime = true, userId = "1", isCompleted = true),
//                Task(id = UUID.randomUUID(), description = "T2", inTime = true, userId = "1", isCompleted = false),
//                Task(id = UUID.randomUUID(), description = "T3", inTime = true, userId = "1", isCompleted = true),
//                Task(id = UUID.randomUUID(), description = "T4", inTime = true, userId = "1", isCompleted = false),
//                Task(id = UUID.randomUUID(), description = "T5", inTime = true, userId = "1", isCompleted = false),
//                Task(id = UUID.randomUUID(), description = "T6", inTime = true, userId = "1", isCompleted = false)
//            )
//
//            every { taskRepository.findAll() } returns tasks
//
//            // Act
//            val response: DashboardResponse = taskService.getDashboardData()
//
//            // Assert
//            assertEquals(6, response.total)
//            assertEquals(2, response.completed)
//            assertEquals(4, response.pending)
//            assertEquals(5, response.recent.size) // Verifica a limitação dos 5 mais recentes
//            assertEquals(tasks.take(5), response.recent)
//
//            verify(exactly = 1) { taskRepository.findAll() }
//        }
//
//        @Test
//        fun `deve retornar dashboard zerado quando nao existirem tarefas`() {
//            // Arrange
//            every { taskRepository.findAll() } returns emptyList()
//            // Act
//            val response = taskService.getDashboardData()
//            // Assert
//            assertEquals(0, response.total)
//            assertEquals(0, response.completed)
//            assertEquals(0, response.pending)
//            assertTrue(response.recent.isEmpty())
//        }
//    }
//
//    @Nested
//    inner class ToggleStatus {
//
//        @Test
//        fun `deve inverter o status de uma tarefa existente de falsa para concluida`() {
//            // Arrange
//            val taskId = UUID.randomUUID()
//            val existingTask = Task(
//                id = taskId,
//                description = "Testar toggle",
//                inTime = true,
//                userId = "123",
//                isCompleted = false
//            )
//
//            every { taskRepository.findById(taskId) } returns Optional.of(existingTask)
//            every { taskRepository.save(any()) } returns existingTask
//
//            // Act
//            taskService.toggleStatus(taskId)
//
//            // Assert
//            assertTrue(existingTask.isCompleted)
//            verify(exactly = 1) { taskRepository.findById(taskId) }
//            verify(exactly = 1) { taskRepository.save(existingTask) }
//        }
//
//        @Test
//        fun `deve lancar excecao quando a tarefa nao for encontrada pelo ID`() {
//            // Arrange
//            val invalidId = UUID.randomUUID()
//            every { taskRepository.findById(invalidId) } returns Optional.empty()
//            // Act & Assert
//            val exception = assertThrows<IllegalArgumentException> {
//                taskService.toggleStatus(invalidId)
//            }
//            assertEquals("Tarefa não encontrada com o ID: $invalidId", exception.message)
//            verify(exactly = 1) { taskRepository.findById(invalidId) }
//            verify(exactly = 0) { taskRepository.save(any()) }
//        }
//
//
//        @Test
//        fun `deve registrar uma multa quando o usuario nao cumprir a tarefa dentro do prazo`() {
//            // Arrange
//            val userId = UUID.randomUUID()
//            val taskId = UUID.randomUUID()
//            val expiredTask = Task(
//                id = taskId,
//                userId = userId,
//                description = "Limpeza da casa",
//                dueDate = LocalDate.now().minusDays(1), // Tarefa expirada
//                completed = false
//            )
//            // Configura os mocks para simular a tarefa pendente e a persistência da multa
//            every { taskRepository.findOverdueTasks() } returns listOf(expiredTask)
//            every { fineRepository.save(any()) } returnsArgument 0
//            // Act
//            taskService.processOverdueTasks()
//            // Assert
//            // Valida que a multa foi criada e associada ao utilizador/tarefa corretos
//            val fineSlot = slot<Fine>()
//            verify(exactly = 1) { fineRepository.save(capture(fineSlot)) }
//            val capturedFine = fineSlot.captured
//            assertEquals(userId, capturedFine.userId)
//            assertEquals(taskId, capturedFine.taskId)
//            assertEquals(BigDecimal("10.00"), capturedFine.amount) // Exemplo de valor da multa
//            // Opcional: Garante que a tarefa também foi atualizada/marcada como penalizada
//            verify(exactly = 1) { taskRepository.save(any()) }
//        }
//    }
//}