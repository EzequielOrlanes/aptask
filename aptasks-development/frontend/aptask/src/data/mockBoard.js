// Dados de exemplo do quadro semanal. O backend ainda não guarda o resultado das
// semanas passadas, então os protótipos usam estes dados fictícios.

export const MOCK_RESIDENTS = [{ name: "André" }, { name: "Gabriela" }, { name: "Ezequiel" }, { name: "Guilherme" }];

const MOCK_TASKS = ["Limpar a cozinha", "Limpar o banheiro", "Varrer a sala", "Lavar a louça"];

export const STATUS_LABELS = {
  done: "Concluída",
  missed: "Não feita",
  pending: "Pendente",
  upcoming: "Em breve",
};

export const STATUS_MARKS = { done: "✓", missed: "✗", pending: "…", upcoming: "·" };

const DAY_MS = 24 * 60 * 60 * 1000;

function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

// Segunda-feira da semana de `date`, a mesma definição de semana do backend.
function mondayOf(date) {
  const day = startOfDay(date);
  const offset = (day.getDay() + 6) % 7;
  return new Date(day.getTime() - offset * DAY_MS);
}

function formatDay(date) {
  return `${String(date.getDate()).padStart(2, "0")}/${String(date.getMonth() + 1).padStart(2, "0")}`;
}

// Semanas (segunda a domingo) que tocam o mês de `today`.
export function getMonthWeeks(today = new Date()) {
  const monthStart = new Date(today.getFullYear(), today.getMonth(), 1);
  const monthEnd = new Date(today.getFullYear(), today.getMonth() + 1, 0);
  const currentMonday = mondayOf(today).getTime();
  const weeks = [];
  for (let monday = mondayOf(monthStart); monday <= monthEnd; monday = new Date(monday.getTime() + 7 * DAY_MS)) {
    const sunday = new Date(monday.getTime() + 6 * DAY_MS);
    weeks.push({
      index: weeks.length,
      label: `Semana ${weeks.length + 1}`,
      range: `${formatDay(monday)} – ${formatDay(sunday)}`,
      isCurrent: monday.getTime() === currentMonday,
      isPast: monday.getTime() < currentMonday,
    });
  }
  return weeks;
}

function statusFor(personIndex, week) {
  if (week.isCurrent) return personIndex % 2 === 0 ? "done" : "pending";
  if (!week.isPast) return "upcoming";
  return (personIndex * 3 + week.index * 2) % 5 === 0 ? "missed" : "done";
}

// { weeks, people: [{ name, cells: [{ week, task, status }] }] }
export function buildMockBoard(today = new Date()) {
  const weeks = getMonthWeeks(today);
  const people = MOCK_RESIDENTS.map((resident, personIndex) => ({
    ...resident,
    cells: weeks.map(week => ({
      week,
      task: MOCK_TASKS[(personIndex + week.index) % MOCK_TASKS.length],
      status: statusFor(personIndex, week),
    })),
  }));
  return { weeks, people };
}
