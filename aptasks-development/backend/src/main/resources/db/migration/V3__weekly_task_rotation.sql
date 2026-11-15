-- Execute no SQL Editor do Supabase depois da migração V2.
-- Uma linha por semana garante que uma semana não seja distribuída duas vezes.
ALTER TABLE tasks
    ADD COLUMN IF NOT EXISTS decision_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS task_rotation_runs (
    week_start DATE PRIMARY KEY,
    executed_at TIMESTAMPTZ NOT NULL
);
