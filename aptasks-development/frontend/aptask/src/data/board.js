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
  previous2: "2 semanas atrás",
  previous: "Semana anterior",
  current: "Esta semana",
  next: "Próxima semana",
  next2: "Daqui a 2 semanas",
};

// "2026-10-12" -> "12/10"
function formatDay(isoDate) {
  const [, month, day] = String(isoDate).split("-");
  return `${day}/${month}`;
}

// "2026-11-26T21:00:00Z" -> "26/11 18:00" (no fuso do navegador)
export function formatDateTime(isoInstant) {
  const date = new Date(isoInstant);
  if (Number.isNaN(date.getTime())) return "";
  const day = date.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" });
  const time = date.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
  return `${day} ${time}`;
}

// { weeks, people: [{ ...morador, cells: [{ week, items: [{ task, status }] }] }] }
export function buildBoard(weeklyBoard, residents = []) {
  const weeks = (weeklyBoard?.weeks || []).map((week, index) => ({
    index,
    label: POSITION_LABELS[week.position] || `Semana ${index + 1}`,
    position: week.position,
    weekStart: week.weekStart,
    weekEnd: week.weekEnd,
    // Prazo prorrogado, só na semana em vigor.
    deadline: week.position === "current" ? weeklyBoard.extendedUntil || null : null,
    range: `${formatDay(week.weekStart)} – ${formatDay(week.weekEnd)}`,
    isCurrent: week.position === "current",
    isPast: week.position === "previous" || week.position === "previous2",
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
