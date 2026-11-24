import React from "react";
import { PersonLabel, StatusBadge, WeekHeading } from "./BoardParts";

// Opção B: uma coluna por semana, com um card por morador.
function BoardColumns({ board }) {
  return (
    <div className="board-scroll board-columns" tabIndex={0} aria-label="Colunas de tarefas por semana">
      {board.weeks.map(week => (
        <section key={week.index} className={`board-column ${week.isCurrent ? "is-current" : ""} ${!week.isPast && !week.isCurrent ? "is-upcoming" : ""}`} aria-label={`${week.label}, ${week.range}`}>
          <header><WeekHeading week={week} /></header>
          {board.people.map(person => {
            const cell = person.cells[week.index];
            return (
              <article className="board-card" key={person.name}>
                <PersonLabel person={person} />
                <span className="board-cell-task">{cell.task}</span>
                <StatusBadge status={cell.status} />
              </article>
            );
          })}
        </section>
      ))}
    </div>
  );
}

export default BoardColumns;
