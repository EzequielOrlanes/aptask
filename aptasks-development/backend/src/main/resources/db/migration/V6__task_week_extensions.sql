-- Execute depois da migração V5.
-- Prorrogação do prazo de uma semana: enquanto o novo prazo não passa, a rotação semanal espera.
CREATE TABLE IF NOT EXISTS task_week_extensions (
    week_start     DATE PRIMARY KEY,
    extended_until TIMESTAMPTZ NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL
);

ALTER TABLE task_week_extensions ENABLE ROW LEVEL SECURITY;
