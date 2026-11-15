-- Execute depois da migração V3.
-- Mantém um registro por tarefa e semana, preservando os textos históricos.
CREATE TABLE IF NOT EXISTS task_rotation_history (
    id UUID PRIMARY KEY,
    task_id UUID NULL REFERENCES tasks(id) ON DELETE SET NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    task_description TEXT NOT NULL,
    user_name TEXT NOT NULL,
    week_start DATE NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_task_rotation_history_task_week UNIQUE (task_id, week_start)
);

CREATE INDEX IF NOT EXISTS idx_task_rotation_history_user_week
    ON task_rotation_history (user_id, week_start DESC);
