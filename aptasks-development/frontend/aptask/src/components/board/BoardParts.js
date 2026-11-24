import React from "react";
import ResidentAvatar from "../ResidentAvatar";
import { STATUS_LABELS, STATUS_MARKS } from "../../data/mockBoard";

export function StatusBadge({ status, compact = false }) {
  return (
    <span className={`task-status board-status is-${status}`} title={STATUS_LABELS[status]}>
      <span aria-hidden="true">{STATUS_MARKS[status]}</span>
      <span className={compact ? "sr-only" : ""}>{STATUS_LABELS[status]}</span>
    </span>
  );
}

export function PersonLabel({ person }) {
  return (
    <span className="board-person">
      <ResidentAvatar resident={person} size="small" />
      <strong>{person.name}</strong>
    </span>
  );
}

export function WeekHeading({ week }) {
  return (
    <span className="board-week-heading">
      <strong>{week.label}</strong>
      <small>{week.range}</small>
      {week.isCurrent && <em>Atual</em>}
    </span>
  );
}
