import React from "react";
import ResidentAvatar from "../ResidentAvatar";
import { STATUS_LABELS, STATUS_MARKS } from "../../data/board";

export function StatusBadge({ status }) {
  return (
    <span className={`task-status board-status is-${status}`} title={STATUS_LABELS[status]}>
      <span aria-hidden="true">{STATUS_MARKS[status]}</span>
      <span>{STATUS_LABELS[status]}</span>
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
