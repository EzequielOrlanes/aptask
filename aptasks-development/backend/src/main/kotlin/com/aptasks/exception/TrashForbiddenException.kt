package com.aptasks.exception

/** Ação da lixeira permitida apenas ao responsável atual (resposta 403). */
class TrashForbiddenException(message: String) : RuntimeException(message)
