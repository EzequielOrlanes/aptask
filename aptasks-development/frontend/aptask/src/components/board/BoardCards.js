import React from "react";
import { STATUS_LABELS } from "../../data/mockBoard";
import { PersonLabel, StatusBadge } from "./BoardParts";

// Opção C: um cartão por morador, com a semana passada, a atual e o mês em bolinhas.
function BoardCards({ board }) {
  const currentIndex = board.weeks.findIndex(week => week.isCurrent);
  return (
    <div className="board-people">
      {board.people.map(person => {
        const current = person.cells[currentIndex];
        const previous = currentIndex > 0 ? person.cells[currentIndex - 1] : null;
        return (
          <article className="board-person-card" key={person.name}>
            <PersonLabel person={person} />
            <div className="board-person-rows">
              <div>
                <small>Semana passada</small>
                {previous ? <><span className="board-cell-task">{previous.task}</span><StatusBadge status={previous.status} /></> : <span className="board-empty">Sem registro neste mês</span>}
              </div>
              <div>
                <small>Esta semana</small>
                {current ? <><span className="board-cell-task">{current.task}</span><StatusBadge status={current.status} /></> : <span className="board-empty">Sem tarefa</span>}
              </div>
            </div>
            <ol className="board-dots" aria-label="Semanas do mês">
              {person.cells.map(cell => (
                <li key={cell.week.index} className={`is-${cell.status} ${cell.week.isCurrent ? "is-current" : ""}`} title={`${cell.week.label}: ${STATUS_LABELS[cell.status]}`}>
                  <span className="sr-only">{cell.week.label}: {STATUS_LABELS[cell.status]}</span>
                </li>
              ))}
            </ol>
          </article>
        );
      })}
    </div>
  );
}

export default BoardCards;
