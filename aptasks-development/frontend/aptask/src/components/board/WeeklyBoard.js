import React, { useMemo } from "react";
import { buildBoard } from "../../data/board";
import Skeleton from "../Skeleton";
import BoardGrid from "./BoardGrid";

// Quadro semanal: carrossel com a semana anterior, a atual e a próxima, vindas do backend.
function WeeklyBoard({ board, residents = [], ready = false }) {
  const model = useMemo(() => buildBoard(board, residents), [board, residents]);

  if (!ready) return <Skeleton variant="block" label="Carregando tarefas…" />;
  if (!model.weeks.length || !model.people.length) {
    return <p className="empty-state">Nenhuma tarefa cadastrada para esta semana.</p>;
  }
  return <BoardGrid board={model} />;
}

export default WeeklyBoard;
