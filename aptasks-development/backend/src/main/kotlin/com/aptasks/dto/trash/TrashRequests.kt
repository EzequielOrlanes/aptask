package com.aptasks.dto.trash

/** Textos em vez de UUID: a validação do identificador fica no service, com mensagens em português. */
data class TrashResponsibleRequest(val residentId: String?)

data class TrashReportRequest(val reportedByResidentId: String?)

data class TrashEmptyRequest(val updatedByResidentId: String?)
