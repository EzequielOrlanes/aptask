-- Execute depois da migração V4.
-- Guarda o resultado de cada tarefa por semana. As colunas ficam NULL até a
-- rotação semanal "selar" a semana; NULL significa "sem registro".
ALTER TABLE task_rotation_history
    ADD COLUMN IF NOT EXISTS is_completed BOOLEAN NULL,
    ADD COLUMN IF NOT EXISTS decision_at  TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS fine_applied BOOLEAN NULL;
