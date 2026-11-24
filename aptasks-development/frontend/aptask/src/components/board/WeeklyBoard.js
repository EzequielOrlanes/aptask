import React, { useMemo, useState } from "react";
import { buildBoard, formatDateTime } from "../../data/board";
import Skeleton from "../Skeleton";
import BoardGrid from "./BoardGrid";
import WeekDeadlineDialog from "./WeekDeadlineDialog";

// Quadro semanal: carrossel com a semana anterior, a atual e a próxima, vindas do backend.
// O administrador (`canEdit`) clica na data da semana em foco para mudar o prazo dela.
function WeeklyBoard({ board, residents = [], ready = false, canEdit = false, onExtendWeek }) {
  const model = useMemo(() => buildBoard(board, residents), [board, residents]);
  const [editing, setEditing] = useState(null);
  const [feedback, setFeedback] = useState("");

  if (!ready) return <Skeleton variant="block" label="Carregando tarefas…" />;
  if (!model.weeks.length || !model.people.length) {
    return <p className="empty-state">Nenhuma tarefa cadastrada para esta semana.</p>;
  }

  const save = async (until, week) => {
    const result = await onExtendWeek(until, week);
    const cancelled = result?.finesCancelled ?? 0;
    const keptPaid = result?.finesKeptPaid ?? 0;
    setFeedback(
      `Prazo salvo até ${formatDateTime(until)}. ${cancelled} multa(s) cancelada(s)` +
      (keptPaid ? `; ${keptPaid} já paga(s) foi(ram) mantida(s).` : ".")
    );
  };

  return (
    <>
      {board?.extendedUntil && (
        <p className="board-notice board-extension" role="status">
          Prazo da semana prorrogado até {formatDateTime(board.extendedUntil)}. A rotação acontece depois desse prazo.
        </p>
      )}
      {feedback && <p className="board-notice board-feedback" role="status">{feedback}</p>}
      <BoardGrid board={model} onEditDeadline={canEdit && onExtendWeek ? week => { setFeedback(""); setEditing(week); } : undefined} />
      {editing && (
        <WeekDeadlineDialog week={editing} extendedUntil={board?.extendedUntil} onSave={save} onClose={() => setEditing(null)} />
      )}
    </>
  );
}

export default WeeklyBoard;
