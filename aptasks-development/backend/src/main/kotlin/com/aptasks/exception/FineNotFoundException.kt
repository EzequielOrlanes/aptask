package com.aptasks.exception

import java.util.UUID

class FineNotFoundException(id: UUID) : RuntimeException("Multa não encontrada: $id")
