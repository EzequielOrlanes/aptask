-- Execute no SQL Editor do Supabase antes de subir a versão nova do backend.
-- Prazo padrão de 7 dias para tarefas já existentes.
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS due_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS fine_applied_at TIMESTAMPTZ;

UPDATE tasks
SET due_at = NOW() + INTERVAL '7 days'
WHERE due_at IS NULL;

ALTER TABLE tasks
    ALTER COLUMN due_at SET NOT NULL;
