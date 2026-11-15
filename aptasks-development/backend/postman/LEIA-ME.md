# Testes da API AP103 no Postman

## Preparar o backend

1. Confirme que as migrations `V2__task_deadlines.sql` e `V3__weekly_task_rotation.sql` foram executadas no Supabase. A rotação usa a tabela criada pela V3.
2. O frontend usa `localhost:3000` no desenvolvimento. Para evitar conflito de porta, inicie o backend em `8080`:

   ```powershell
   .\gradlew.bat bootRun --args="--server.port=8080"
   ```

3. Espere o Spring informar que iniciou. O `.env` do backend precisa conter as configurações de conexão do banco; não coloque essas chaves no Postman.

## Importar e executar

1. No Postman, use **Import** e selecione `AP103.postman_collection.json` e `AP103.postman_environment.json`.
2. Selecione o ambiente **AP103 local**.
3. Execute primeiro a pasta **1. Moradores**. O teste salva automaticamente o primeiro `residentId` encontrado.
4. Abra o **Collection Runner** e execute somente a pasta **2. CRUD de tarefas**, em ordem. Ela cria uma tarefa identificada como `[Postman]`, consulta, edita, verifica a distribuição semanal, marca como concluída, apaga e confirma o `404` esperado depois da exclusão.
5. A pasta **3. Presença** é manual: ela altera o estado real do morador para o valor da variável `isAtHome` (`true` ou `false`). Ajuste a variável antes de enviar.

## Rotas cobertas

- `GET /residents`
- `GET /tasks`, `POST /tasks/create`, `GET /tasks/{id}`, `PUT /tasks/{id}`, `DELETE /tasks/{id}`
- `GET /tasks/weekly`, `PATCH /tasks/{id}/decision`
- `PATCH /residents/{id}/presence`

O scheduler de atraso/multa roda em segundo plano. A collection não força o relógio nem cria uma tarefa vencida, então ela não valida por si só a aplicação automática da multa. Isso precisa ser exercitado com uma tarefa vencida no banco e o scheduler ativo, ou com um mecanismo de teste dedicado.
