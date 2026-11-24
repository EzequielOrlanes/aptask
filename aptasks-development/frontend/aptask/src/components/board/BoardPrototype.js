import React, { useMemo, useState } from "react";
import { buildMockBoard } from "../../data/mockBoard";
import BoardGrid from "./BoardGrid";
import BoardColumns from "./BoardColumns";
import BoardCards from "./BoardCards";

const VIEWS = [
  { id: "list", label: "Lista atual" },
  { id: "grid", label: "A · Grade" },
  { id: "columns", label: "B · Colunas" },
  { id: "cards", label: "C · Cartões" },
];

// Seletor entre a lista atual e os três protótipos do quadro semanal.
// `listView` é a lista de tarefas que já existia na Home.
function BoardPrototype({ listView, today }) {
  const [view, setView] = useState("list");
  const board = useMemo(() => buildMockBoard(today), [today]);
  const gridBoard = useMemo(() => buildMockBoard(today, { surrounding: true }), [today]);

  return (
    <>
      <div className="board-tabs" role="tablist" aria-label="Visualização do quadro">
        {VIEWS.map(item => (
          <button key={item.id} type="button" role="tab" aria-selected={view === item.id} className={view === item.id ? "is-active" : ""} onClick={() => setView(item.id)}>
            {item.label}
          </button>
        ))}
      </div>
      {view !== "list" && <p className="board-notice">Protótipo com dados de exemplo, não são as tarefas reais da casa.</p>}
      {view === "list" && listView}
      {view === "grid" && <BoardGrid board={gridBoard} />}
      {view === "columns" && <BoardColumns board={board} />}
      {view === "cards" && <BoardCards board={board} />}
    </>
  );
}

export default BoardPrototype;
