import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Home from "./Home";

const renderHome = props => render(
  <MemoryRouter>
    <Home loggedUser="" setLoggedUser={() => {}} residents={[]} tasks={[]} {...props} />
  </MemoryRouter>
);

test("sem dados do backend mostra esqueletos e nenhum morador inventado", () => {
  renderHome({ ready: { tasks: false, residents: false, trash: false } });

  expect(screen.getByText("Carregando moradores…")).toBeInTheDocument();
  expect(screen.getByText("Carregando tarefas…")).toBeInTheDocument();
  expect(screen.getByText("Carregando status do lixo…")).toBeInTheDocument();
  expect(screen.queryByText("Ezequiel")).not.toBeInTheDocument();
  expect(screen.queryByText(/Lixeira/)).not.toBeInTheDocument();
});

test("com dados do backend mostra moradores e tarefas reais", () => {
  renderHome({
    ready: { tasks: true, residents: true, trash: true },
    residents: [{ id: "1", name: "Ana", nickname: "Aninha", initials: "AN" }],
    tasks: [{ id: "t1", task: "Lavar louça", userName: "Ana", status: false }],
    trashStatus: { fillLevel: "EMPTY" },
  });

  expect(screen.getByRole("link", { name: /Ana/ })).toBeInTheDocument();
  expect(screen.getByText("Lavar louça")).toBeInTheDocument();
  expect(screen.queryByText("Carregando moradores…")).not.toBeInTheDocument();
});
