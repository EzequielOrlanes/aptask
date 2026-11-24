// Converte a resposta de GET /tasks/board no formato que o carrossel desenha.

export const STATUS_LABELS = {
  done: "Concluída",
  missed: "Não feita",
  pending: "Pendente",
  upcoming: "Em breve",
  unknown: "Sem registro",
};

export const STATUS_MARKS = { done: "✓", missed: "✗", pending: "…", upcoming: "·", unknown: "?" };

const POSITION_LABELS = {
  previous: "Semana anterior",
  current: "Esta semana",
  next: "Próxima semana",
};

// "2026-10-12" -> "12/10"
function formatDay(isoDate) {
  const [, month, day] = String(isoDate).split("-");
  return `${day}/${month}`;
}

// { weeks, people: [{ ...morador, cells: [{ week, items: [{ task, status }] }] }] }
export function buildBoard(weeklyBoard, residents = []) {
  const weeks = (weeklyBoard?.weeks || []).map((week, index) => ({
    index,
    label: POSITION_LABELS[week.position] || `Semana ${index + 1}`,
    range: `${formatDay(week.weekStart)} – ${formatDay(week.weekEnd)}`,
    isCurrent: week.position === "current",
    isPast: week.position === "previous",
    entries: week.entries || [],
  }));

  const people = residents.map(resident => ({
    ...resident,
    cells: weeks.map(week => ({
      week,
      items: week.entries
        .filter(entry => entry.userId === resident.id)
        .map(entry => ({ task: entry.taskDescription, status: entry.status })),
    })),
  }));

  return { weeks, people };
}
