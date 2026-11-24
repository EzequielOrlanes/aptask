import React, { useState } from "react";
import { PersonLabel, StatusBadge } from "./BoardParts";

// Opção A: carrossel de semanas. A semana ativa fica em foco no centro e as
// vizinhas ficam desfocadas atrás; clicar numa delas a traz para o centro.
function BoardGrid({ board }) {
  const initial = Math.max(0, board.weeks.findIndex(week => week.isCurrent));
  const [active, setActive] = useState(initial);
  const last = board.weeks.length - 1;
  const go = index => setActive(Math.min(last, Math.max(0, index)));

  const onKeyDown = event => {
    if (event.key === "ArrowLeft") { event.preventDefault(); go(active - 1); }
    if (event.key === "ArrowRight") { event.preventDefault(); go(active + 1); }
  };

  return (
    <div className="board-carousel" onKeyDown={onKeyDown}>
      <div className="board-carousel-stage" role="group" aria-roledescription="carrossel" aria-label="Tarefas por semana">
        {board.weeks.map((week, index) => {
          const offset = index - active;
          const isActive = offset === 0;
          const select = () => go(index);
          return (
            <section
              key={week.index}
              className={`board-slide ${isActive ? "is-active" : ""} ${week.isCurrent ? "is-current-week" : ""}`}
              data-offset={Math.max(-2, Math.min(2, offset))}
              aria-label={`${week.label}, ${week.range}`}
              aria-current={isActive ? "true" : undefined}
              role={isActive ? undefined : "button"}
              tabIndex={isActive ? undefined : 0}
              onClick={isActive ? undefined : select}
              onKeyDown={isActive ? undefined : event => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); select(); } }}
            >
              <div className="board-slide-body" aria-hidden={isActive ? undefined : "true"}>
                <header className="board-slide-head">
                  <span>
                    <strong>{week.label}</strong>
                    <small>{week.range}</small>
                  </span>
                  {week.isCurrent && <em>Atual</em>}
                </header>
                <ul className="board-slide-list">
                  {board.people.map(person => {
                    const cell = person.cells[week.index];
                    return (
                      <li key={person.name}>
                        <PersonLabel person={person} />
                        <span className="board-cell-task">{cell.task}</span>
                        <StatusBadge status={cell.status} />
                      </li>
                    );
                  })}
                </ul>
              </div>
            </section>
          );
        })}
      </div>
      <div className="board-carousel-nav">
        <button type="button" onClick={() => go(active - 1)} disabled={active === 0} aria-label="Semana anterior">‹</button>
        <ol aria-label="Semanas">
          {board.weeks.map((week, index) => (
            <li key={week.index}>
              <button type="button" className={index === active ? "is-active" : ""} onClick={() => go(index)} aria-label={`Ir para ${week.label} (${week.range})`} aria-pressed={index === active} />
            </li>
          ))}
        </ol>
        <button type="button" onClick={() => go(active + 1)} disabled={active === last} aria-label="Próxima semana">›</button>
      </div>
    </div>
  );
}

export default BoardGrid;
