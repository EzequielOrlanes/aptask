# API AP103: entradas e saídas

Referência das rotas do backend, com entrada, saída e erros. O contrato formal está em [openapi.yaml](openapi.yaml), que pode ser aberto no [Swagger Editor](https://editor.swagger.io) (colar o conteúdo do arquivo) ou em qualquer visualizador de OpenAPI 3.0.

**Base:** `https://apptasks-e6oh.onrender.com` em produção, `http://localhost:8080` em desenvolvimento.

## Convenções

**Envelope de resposta.** Toda resposta é um `ApiResponse`:

```json
{
  "success": true,
  "message": "Distribuição semanal carregada.",
  "data": { },
  "count": 4,
  "timestamp": null
}
```

Em erro, `success` é `false` e o texto fica em `error`:

```json
{ "success": false, "error": "Morador não encontrado." }
```

**Identificadores.** Ids são UUIDs em texto. Um id inválido gera `400`, com mensagem em português.

**Autenticação.** Rotas administrativas exigem o cabeçalho `Authorization: Bearer <token>`. Sem token ou com token inválido, a resposta é `401` com `{"success": false, "error": "Acesso restrito ao administrador."}`.

**Erros comuns.**

| Status | Quando |
|---|---|
| `400` | Dados inválidos ou faltando (`IllegalArgumentException`) |
| `401` | Token ausente ou inválido, ou login de administrador recusado |
| `403` | Ação restrita a outro morador (somente no esvaziamento do lixo) |
| `404` | Tarefa ou multa não encontrada |
| `409` | Estado não permite a ação (lixo já vazio) |

---

## Auth

### `POST /auth/admin/login`

Entrada:

```json
{ "email": "ezequiel@aptasks.local" }
```

Saída `200`: `data` com `token` (string `payload.assinatura`) e `expiresAt` (ISO-8601). Validade de 168 horas por padrão.

Erros: `401` se o e-mail não for o do morador `ADMIN_NAME`.

---

## Moradores

### `GET /residents`

Sem entrada. Saída: `data` é uma lista de `{ id, name, isAtHome }`.

### `PATCH /residents/{id}/presence`

Entrada: `{ "isAtHome": true }`.

Saída: `data` com o morador atualizado.

Erros: `400` com `"Morador não encontrado."` se o id não existir.

---

## Tarefas

### `GET /tasks/weekly`

Sem entrada. Antes de responder, executa a rotação da semana se ela ainda não tiver sido feita.

Saída: `data` é uma lista de tarefas da semana:

```json
{
  "id": "uuid",
  "description": "Banheiro social",
  "userId": "uuid",
  "userName": "André Borel",
  "weekStart": "2026-11-16",
  "deadline": "2026-11-23T02:59:59Z",
  "status": "pending",
  "decisionRecorded": false,
  "fineApplied": false,
  "fineAmount": null
}
```

`status` é `pending` ou `done`, derivado de `isCompleted`.

### `PATCH /tasks/{id}/decision`

Entrada: `{ "done": true }`.

Saída: `data` com a tarefa semanal atualizada (mesmo formato de `GET /tasks/weekly`).

### `PATCH /tasks/{id}/toggle`

Sem entrada. Alterna `isCompleted`. Saída: `success: true` e a confirmação em `message` ("Status da tarefa atualizado com sucesso"). `data` fica vazio.

### `GET /tasks/history?userId={uuid}`

`userId` é opcional. Saída: lista de `{ taskId, taskDescription, userId, userName, weekStart, assignedAt }`, da semana mais recente para a mais antiga.

### `GET /tasks` e `GET /tasks/read`

Alias. Saída: lista de tarefas no formato da entidade: `{ id, description, inTime, userId, isCompleted, dueAt, fineAppliedAt, decisionAt }`.

### `GET /tasks/{id}`

Saída: uma tarefa no mesmo formato. Erro `404` se não existir.

### `POST /tasks/create` (administrador)

Entrada:

```json
{ "responsible_user": "uuid", "description": "Sala, sofá", "dueAt": "2026-12-01T20:00:00-03:00" }
```

`dueAt` é opcional e precisa estar no futuro. Saída `201` com a tarefa criada.

### `PUT /tasks/{id}` (administrador)

Entrada: mesmo formato de `POST /tasks/create`. Saída: a tarefa atualizada. Erros `400` e `404`.

### `DELETE /tasks/{id}` (administrador)

Sem entrada. Saída: `success` e `message` de confirmação. Erro `404` se não existir.

---

## Lixo

### `GET /trash/today`

Saída: `data` com o estado atual:

```json
{
  "responsible": { "id": "uuid", "name": "André Borel", "isAtHome": true },
  "fillLevel": "HALF",
  "isFull": false,
  "reportedAt": "2026-11-16T11:46:49Z",
  "reportedBy": { "id": "uuid", "name": "Ezequiel", "isAtHome": true }
}
```

`fillLevel` é `EMPTY`, `HALF` ou `FULL`.

### `PATCH /trash/responsible`

Entrada: `{ "residentId": "uuid" }`. Saída: estado do lixo. Erro `400` se o morador não existir.

### `POST /trash/half` e `POST /trash/full`

Entrada: `{ "reportedByResidentId": "uuid" }`. Saída: estado do lixo.

Um aviso de meio cheio só é gravado com o lixo vazio, e um de cheio só se ainda não estiver cheio. Avisos repetidos respondem o estado atual sem gravar.

### `POST /trash/empty`

Entrada: `{ "updatedByResidentId": "uuid" }`. Saída: estado do lixo com `fillLevel` `EMPTY`.

Erros: `403` se o morador não for o responsável atual. `409` se o lixo já estiver vazio.

---

## Multas

### `POST /fines`

Entrada:

```json
{ "amount": 5.0, "sender": "uuid", "receiver": "uuid" }
```

`timestamp` é opcional e assume o instante atual. `amount` precisa ser maior que zero e não pode passar do limite configurado. Remetente e destinatário precisam ser moradores diferentes e existentes.

Saída: `data` com a multa criada: `{ id, timestamp, amount, sender, receiver, isPaid }`.

### `GET /fines?userId={uuid}`

Obrigatório `userId`. Saída: multas enviadas e recebidas pelo morador, com `direction` indicando o papel dele. Cada item tem `{ id, timestamp, amount, senderId, senderName, receiverId, receiverName, isPaid, direction }`.

### `PATCH /fines/{id}/paid`

Sem entrada. Marca a multa como paga. Saída: a multa. Erro `404` se não existir.

### `GET /fines/admin` (administrador)

Sem entrada. Saída: todas as multas, no mesmo formato de `GET /fines`.

### `DELETE /fines/{id}` (administrador)

Sem entrada. Saída: confirmação. Erro `404` se não existir.

---

## Rotas que o frontend não usa

- `GET /api/v1/hello`: rota de teste do `TestController`.

## Automações que alteram dados sem chamada HTTP

| Job | Efeito |
|---|---|
| Rotação semanal (00:10 em São Paulo, ou no primeiro `GET /tasks/weekly`) | Redistribui as tarefas da semana e grava o histórico |
| Avaliação de atrasos (a cada hora) | Aplica multa automática em tarefas vencidas e não feitas |

Detalhes em [arquitetura.md](arquitetura.md), seção 7.
