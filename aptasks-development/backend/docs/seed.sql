-- =============================================================
-- Dados iniciais da casa: 4 moradores e 4 tarefas de limpeza.
-- Execute no SQL Editor do Supabase depois do schema.sql.
-- Idempotente: pode rodar de novo sem duplicar registros.
-- Os e-mails são placeholders; troque se quiser.
-- =============================================================

INSERT INTO users (id, name, email, is_at_home, homework_is_done) VALUES
  ('12d388c3-bab1-458b-b992-a8217d074c57', 'Gabriela', 'gabriela@aptasks.local', FALSE, FALSE),
  ('04509a86-badd-46ea-9ad1-b9fdf0ac745e', 'Isabella', 'isabella@aptasks.local', FALSE, FALSE),
  ('09994706-a062-4456-989b-522bc5edabb5', 'Ezequiel', 'ezequiel@aptasks.local', FALSE, FALSE),
  ('38d8316b-5d8f-4581-9c0c-dd6f1a26c442', 'Guilherme', 'guilherme@aptasks.local', FALSE, FALSE)
ON CONFLICT (id) DO NOTHING;

-- Tarefas com prazo de 7 dias. A rotação semanal reatribui os responsáveis
-- na primeira leitura de /tasks/weekly.
INSERT INTO tasks (id, description, in_time, user_id, is_completed, due_at) VALUES
  ('bccfccb0-726c-4620-baaf-664cad41639b', 'Sala, sofá, corredor, mesa, varanda', TRUE, '09994706-a062-4456-989b-522bc5edabb5', FALSE, NOW() + INTERVAL '7 days'),
  ('56b4d032-3522-4409-89cd-9a0fc9919721', 'Banheiro social', TRUE, '12d388c3-bab1-458b-b992-a8217d074c57', FALSE, NOW() + INTERVAL '7 days'),
  ('9bf276ee-5710-49bf-b6f4-e344ffdf16ff', 'Pia, geladeira, fogão, microondas, armários cozinha, panos de prato', TRUE, '04509a86-badd-46ea-9ad1-b9fdf0ac745e', FALSE, NOW() + INTERVAL '7 days'),
  ('15028a70-d6c7-45bc-aa72-913d913e6ef4', 'Chão cozinha, tanque + armário tanque, lixo, banheiro quartinho + quartinho', TRUE, '38d8316b-5d8f-4581-9c0c-dd6f1a26c442', FALSE, NOW() + INTERVAL '7 days')
ON CONFLICT (id) DO NOTHING;
