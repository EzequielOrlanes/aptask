import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";

import WeeklyBoard from "./WeeklyBoard";
import { buildBoard } from "../../data/board";

const ANA = { id: "ana", name: "Ana" };
const BIA = { id: "bia", name: "Bia" };

const response = {
  weeks: [
    { weekStart: "2026-10-05", weekEnd: "2026-10-11", position: "previous", entries: [
      { userId: "ana", userName: "Ana", taskDescription: "Lixo", status: "done" },
      { userId: "bia", userName: "Bia", taskDescription: "Louça", status: "missed" },
      { userId: "bia", userName: "Bia", taskDescription: "Sala", status: "unknown" },
    ] },
    { weekStart: "2026-10-12", weekEnd: "2026-10-18", position: "current", entries: [
      { userId: "ana", userName: "Ana", taskDescription: "Louça", status: "pending" },
    ] },
    { weekStart: "2026-10-19", weekEnd: "2026-10-25", position: "next", entries: [
      { userId: "bia", userName: "Bia", taskDescription: "Lixo", status: "upcoming" },
    ] },
  ],
};

describe("buildBoard", () => {
  it("monta as três semanas com rótulos, intervalo e semana atual marcada", () => {
    const { weeks } = buildBoard(response, [ANA, BIA]);
    expect(weeks.map(week => week.label)).toEqual(["Semana anterior", "Esta semana", "Próxima semana"]);
    expect(weeks[1].range).toBe("12/10 – 18/10");
    expect(weeks.map(week => week.isCurrent)).toEqual([false, true, false]);
  });

  it("agrupa as tarefas de cada morador por semana, inclusive várias na mesma semana", () => {
    const { people } = buildBoard(response, [ANA, BIA]);
    expect(people[1].cells[0].items).toEqual([
      { task: "Louça", status: "missed" },
      { task: "Sala", status: "unknown" },
    ]);
    expect(people[0].cells[2].items).toEqual([]);
  });

  it("não inventa semanas nem moradores sem dados", () => {
    expect(buildBoard(null, []).weeks).toEqual([]);
    expect(buildBoard(response, []).people).toEqual([]);
  });
});

describe("WeeklyBoard", () => {
  const renderBoard = props => render(<WeeklyBoard board={response} residents={[ANA, BIA]} ready {...props} />);

  it("mostra o esqueleto enquanto o backend não respondeu", () => {
    renderBoard({ ready: false, board: null });
    expect(screen.getByText("Carregando tarefas…")).toBeInTheDocument();
    expect(screen.queryByText("Ana")).not.toBeInTheDocument();
  });

  it("avisa quando não há tarefas", () => {
    renderBoard({ board: { weeks: [] } });
    expect(screen.getByText("Nenhuma tarefa cadastrada para esta semana.")).toBeInTheDocument();
  });

  it("mostra a semana atual em foco e as vizinhas desfocadas", () => {
    renderBoard();
    expect(screen.queryByText(/Dados de exemplo/)).not.toBeInTheDocument();
    const current = screen.getByRole("region", { name: /Esta semana/ });
    expect(current).toHaveAttribute("aria-current", "true");
    expect(current).toHaveAttribute("data-offset", "0");
    expect(screen.getByRole("button", { name: /Semana anterior, / })).toHaveAttribute("data-offset", "-1");
    expect(screen.getByRole("button", { name: /Próxima semana, / })).toHaveAttribute("data-offset", "1");
  });

  it("mostra o resultado da semana anterior ao trazê-la para o foco", () => {
    renderBoard();
    fireEvent.click(screen.getByRole("button", { name: /Semana anterior, / }));
    const previous = screen.getByRole("region", { name: /Semana anterior/ });
    expect(previous).toHaveAttribute("data-offset", "0");
    expect(previous).toHaveTextContent("Lixo");
    expect(previous).toHaveTextContent("Concluída");
    expect(previous).toHaveTextContent("Não feita");
    expect(previous).toHaveTextContent("Sem registro");
  });

  it("indica quem não tem tarefa na semana", () => {
    renderBoard();
    expect(screen.getByRole("region", { name: /Esta semana/ })).toHaveTextContent("Sem tarefa");
  });

  it("navega pelos botões e para nas pontas", () => {
    renderBoard();
    fireEvent.click(screen.getByRole("button", { name: "Próxima semana" }));
    expect(screen.getByRole("button", { name: "Próxima semana" })).toBeDisabled();
    expect(screen.getByRole("region", { name: /Próxima semana/ })).toHaveAttribute("data-offset", "0");
  });
});
