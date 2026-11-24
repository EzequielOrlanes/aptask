import React, { useEffect, useState } from "react";
import { formatDateTime } from "../../data/board";

const pad = number => String(number).padStart(2, "0");

// Valor de <input type="datetime-local"> (horário local) para uma data.
function toInputValue(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

// Prazo que a semana tem hoje: o prorrogado, se houver, ou o fim do domingo.
function currentDeadline(week, extendedUntil) {
  if (week.position === "current" && extendedUntil) return new Date(extendedUntil);
  const [year, month, day] = String(week.weekEnd).split("-").map(Number);
  return new Date(year, month - 1, day, 23, 59);
}

// Se o prazo atual já passou, sugere amanhã às 23:59.
function suggestedValue(week, extendedUntil) {
  const deadline = currentDeadline(week, extendedUntil);
  if (deadline.getTime() > Date.now()) return toInputValue(deadline);
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  tomorrow.setHours(23, 59, 0, 0);
  return toInputValue(tomorrow);
}

const CONSEQUENCES = {
  current: "As multas automáticas desta semana serão canceladas e a rotação só acontece depois do novo prazo.",
  previous: "Esta semana volta a ser a atual até o novo prazo, e a semana atual passa a ser a próxima. As multas aplicadas quando ela venceu serão canceladas.",
};

// Popup para mudar o prazo de uma semana: escolhe a data e responde se pode salvar.
function WeekDeadlineDialog({ week, extendedUntil, onSave, onClose }) {
  const [value, setValue] = useState(() => suggestedValue(week, extendedUntil));
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const onKey = event => { if (event.key === "Escape" && !working) onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose, working]);

  const chosen = value ? new Date(value) : null;
  const valid = chosen && !Number.isNaN(chosen.getTime()) && chosen.getTime() > Date.now();

  const save = async () => {
    if (!valid) return;
    setWorking(true);
    setError("");
    try {
      await onSave(chosen.toISOString(), week.position);
      onClose();
    } catch {
      setError("Não foi possível salvar o novo prazo. Verifique a conexão e tente de novo.");
      setWorking(false);
    }
  };

  return (
    <div className="board-dialog-backdrop" onMouseDown={() => { if (!working) onClose(); }}>
      <div className="board-dialog" role="dialog" aria-modal="true" aria-labelledby="deadline-dialog-title" onMouseDown={event => event.stopPropagation()}>
        <h3 id="deadline-dialog-title">Editar o prazo da {week.label.toLowerCase()}</h3>
        <p className="board-dialog-range">{week.range}</p>

        <label className="board-dialog-field" htmlFor="deadline-dialog-input">
          <span>Novo prazo</span>
          <input id="deadline-dialog-input" type="datetime-local" value={value} disabled={working} autoFocus
            onChange={event => { setValue(event.target.value); setError(""); }} />
        </label>

        <p className="board-dialog-warning">{CONSEQUENCES[week.position]}</p>
        {value && !valid && <p className="board-dialog-error" role="alert">Escolha uma data e hora no futuro.</p>}
        {error && <p className="board-dialog-error" role="alert">{error}</p>}

        <p className="board-dialog-question">
          Salvar o prazo até <strong>{valid ? formatDateTime(chosen.toISOString()) : "…"}</strong>?
        </p>
        <div className="board-dialog-actions">
          <button type="button" className="board-dialog-save" disabled={!valid || working} onClick={save}>{working ? "Salvando…" : "Salvar"}</button>
          <button type="button" className="board-dialog-cancel" disabled={working} onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  );
}

export default WeekDeadlineDialog;
