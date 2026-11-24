import React from "react";
import { PersonLabel, StatusBadge, WeekHeading } from "./BoardParts";

// Opção A: linhas = moradores, colunas = semanas do mês.
function BoardGrid({ board }) {
  return (
    <div className="board-scroll" tabIndex={0} aria-label="Grade de tarefas por morador e semana">
      <table className="board-grid">
        <thead>
          <tr>
            <th scope="col" className="board-sticky">Morador</th>
            {board.weeks.map(week => (
              <th scope="col" key={week.index} className={week.isCurrent ? "is-current" : ""}><WeekHeading week={week} /></th>
            ))}
          </tr>
        </thead>
        <tbody>
          {board.people.map(person => (
            <tr key={person.name}>
              <th scope="row" className="board-sticky"><PersonLabel person={person} /></th>
              {person.cells.map(cell => (
                <td key={cell.week.index} className={`${cell.week.isCurrent ? "is-current" : ""} ${cell.status === "upcoming" ? "is-upcoming" : ""}`}>
                  <span className="board-cell-task">{cell.task}</span>
                  <StatusBadge status={cell.status} />
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default BoardGrid;
