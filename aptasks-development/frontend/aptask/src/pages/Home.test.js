import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Home from "./Home";

const renderHome = props => render(
  <MemoryRouter>
    <Home loggedUser="" setLoggedUser={() => {}} residents={[]} tasks={[]} {...props} />
  </MemoryRouter>
);

test("sem dados do backend mostra esqueletos e nenhum morador inventado", () => {
  renderHome({ ready: { tasks: false, board: false, residents: false, trash: false } });

  expect(screen.getByText("Carregando moradores…")).toBeInTheDocument();
  expect(screen.getByText("Carregando tarefas…")).toBeInTheDocument();
  expect(screen.getByText("Carregando status do lixo…")).toBeInTheDocument();
  expect(screen.queryByText("Ezequiel")).not.toBeInTheDocument();
  expect(screen.queryByText(/Lixeira/)).not.toBeInTheDocument();
});

test("com dados do backend mostra moradores e tarefas reais", () => {
  renderHome({
    ready: { tasks: true, board: true, residents: true, trash: true },
    residents: [{ id: "1", name: "Ana", nickname: "Aninha", initials: "AN" }],
    board: { weeks: [{ weekStart: "2026-10-12", weekEnd: "2026-10-18", position: "current", entries: [
      { userId: "1", userName: "Ana", taskDescription: "Lavar louça", status: "pending" },
    ] }] },
    trashStatus: { fillLevel: "EMPTY" },
  });

  expect(screen.getByRole("link", { name: /Ana/ })).toBeInTheDocument();
  expect(screen.getByText("Lavar louça")).toBeInTheDocument();
  expect(screen.queryByText("Carregando moradores…")).not.toBeInTheDocument();
});

test("só o administrador pode editar o prazo da semana pela data do carrossel", () => {
  const ready = { tasks: true, board: true, residents: true, trash: true };
  const residents = [{ id: "1", name: "Ana" }];
  const board = { weeks: [
    { weekStart: "2026-10-05", weekEnd: "2026-10-11", position: "previous", entries: [] },
    { weekStart: "2026-10-12", weekEnd: "2026-10-18", position: "current", entries: [] },
    { weekStart: "2026-10-19", weekEnd: "2026-10-25", position: "next", entries: [] },
  ] };
  const { unmount } = renderHome({ ready, residents, board, isAdmin: false, onExtendWeek: jest.fn() });
  expect(screen.queryByRole("button", { name: /Editar o prazo/ })).not.toBeInTheDocument();
  unmount();

  renderHome({ ready, residents, board, isAdmin: true, onExtendWeek: jest.fn(), onSaveTask: jest.fn(), onDeleteTask: jest.fn() });
  expect(screen.getByRole("button", { name: /Editar o prazo da esta semana/ })).toBeInTheDocument();
});
