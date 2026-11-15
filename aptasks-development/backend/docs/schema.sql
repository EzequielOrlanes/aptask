-- =============================================================
-- Schema do AP Tasks para o PostgreSQL do Supabase.
-- Execute no SQL Editor do Supabase, em um banco vazio.
-- Idempotente: pode rodar de novo sem apagar dados.
-- Consolida as migrações V2, V3 e V4 e a tabela fine.
-- =============================================================

-- Moradores
CREATE TABLE IF NOT EXISTS users (
    id                UUID PRIMARY KEY,
    name              TEXT NOT NULL,
    email             TEXT NOT NULL UNIQUE,
    is_at_home        BOOLEAN NOT NULL DEFAULT FALSE,
    homework_is_done  BOOLEAN NOT NULL DEFAULT FALSE
);

-- Tarefas da casa
-- user_id é TEXT porque o backend grava o UUID do morador como string.
CREATE TABLE IF NOT EXISTS tasks (
    id               UUID PRIMARY KEY,
    description      TEXT NOT NULL,
    in_time          BOOLEAN NOT NULL DEFAULT TRUE,
    user_id          TEXT NOT NULL,
    is_completed     BOOLEAN NOT NULL DEFAULT FALSE,
    due_at           TIMESTAMPTZ NOT NULL,
    fine_applied_at  TIMESTAMPTZ NULL,
    decision_at      TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS idx_tasks_due_at ON tasks (due_at);
CREATE INDEX IF NOT EXISTS idx_tasks_user_id ON tasks (user_id);

-- Multas
CREATE TABLE IF NOT EXISTS fine (
    id          UUID PRIMARY KEY,
    timestamp   TIMESTAMP NOT NULL,
    amount      NUMERIC(10, 2) NOT NULL,
    sender      UUID NOT NULL REFERENCES users(id),
    receiver    UUID NOT NULL REFERENCES users(id),
    is_paid     BOOLEAN NOT NULL DEFAULT FALSE
);

-- Controle de rotação: uma linha por semana, para não distribuir duas vezes
CREATE TABLE IF NOT EXISTS task_rotation_runs (
    week_start   DATE PRIMARY KEY,
    executed_at  TIMESTAMPTZ NOT NULL
);

-- Histórico de atribuições: uma linha por tarefa e semana
CREATE TABLE IF NOT EXISTS task_rotation_history (
    id                UUID PRIMARY KEY,
    task_id           UUID NULL REFERENCES tasks(id) ON DELETE SET NULL,
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    task_description  TEXT NOT NULL,
    user_name         TEXT NOT NULL,
    week_start        DATE NOT NULL,
    assigned_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_task_rotation_history_task_week UNIQUE (task_id, week_start)
);

CREATE INDEX IF NOT EXISTS idx_task_rotation_history_user_week
    ON task_rotation_history (user_id, week_start DESC);

-- Acesso: o backend conecta como postgres, e a API pública do Supabase
-- (chave publishable) não deve enxergar essas tabelas. Ativar RLS sem
-- políticas bloqueia o acesso via REST para anon e authenticated.
ALTER TABLE users                  ENABLE ROW LEVEL SECURITY;
ALTER TABLE tasks                  ENABLE ROW LEVEL SECURITY;
ALTER TABLE fine                   ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_rotation_runs     ENABLE ROW LEVEL SECURITY;
ALTER TABLE task_rotation_history  ENABLE ROW LEVEL SECURITY;
