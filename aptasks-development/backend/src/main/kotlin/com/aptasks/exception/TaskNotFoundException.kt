package com.aptasks.exception

import java.util.UUID

class TaskNotFoundException(id: UUID) : RuntimeException("Tarefa não encontrada com o ID: $id")
