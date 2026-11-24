import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";

import BoardPrototype from "./BoardPrototype";
import { getMonthWeeks, getSurroundingWeeks, buildMockBoard } from "../../data/mockBoard";

// Quarta-feira, 12/11/2025: novembro de 2025 tem 5 semanas (27/10 a 30/11).
const TODAY = new Date(2025, 10, 12);

describe("getMonthWeeks", () => {
  it("monta semanas de segunda a domingo que tocam o mês e marca a atual", () => {
    const weeks = getMonthWeeks(TODAY);
    expect(weeks).toHaveLength(5);
    expect(weeks[0].range).toBe("27/10 – 02/11");
    expect(weeks.filter(week => week.isCurrent)).toHaveLength(1);
    expect(weeks[2].isCurrent).toBe(true);
  });
});

describe("getSurroundingWeeks", () => {
  it("devolve semana anterior, atual e próxima, mesmo cruzando o mês", () => {
    const weeks = getSurroundingWeeks(new Date(2025, 10, 26));
    expect(weeks.map(week => week.label)).toEqual(["Semana anterior", "Esta semana", "Próxima semana"]);
    expect(weeks[1].range).toBe("24/11 – 30/11");
    expect(weeks[2].range).toBe("01/12 – 07/12");
    expect(weeks.map(week => week.isCurrent)).toEqual([false, true, false]);
  });
});

describe("buildMockBoard", () => {
  it("dá a cada morador uma célula por semana", () => {
    const board = buildMockBoard(TODAY);
    board.people.forEach(person => expect(person.cells).toHaveLength(board.weeks.length));
    expect(board.people[0].cells[4].status).toBe("upcoming");
  });
});

describe("BoardPrototype", () => {
  const renderBoard = () => render(<BoardPrototype today={TODAY} listView={<p>lista real</p>} />);

  it("começa na lista atual, sem aviso de protótipo", () => {
    renderBoard();
    expect(screen.getByText("lista real")).toBeInTheDocument();
    expect(screen.queryByText(/Protótipo com dados de exemplo/)).not.toBeInTheDocument();
  });

  it("mostra o carrossel com a semana atual em foco e as vizinhas desfocadas", () => {
    renderBoard();
    fireEvent.click(screen.getByRole("tab", { name: /Grade/ }));
    expect(screen.getByText(/Protótipo com dados de exemplo/)).toBeInTheDocument();
    expect(screen.getAllByText("Atual")).toHaveLength(1);
    const current = screen.getByRole("region", { name: /Esta semana/ });
    expect(current).toHaveAttribute("aria-current", "true");
    expect(current).toHaveAttribute("data-offset", "0");
    expect(screen.getByRole("button", { name: /Semana anterior, / })).toHaveAttribute("data-offset", "-1");
    expect(screen.getByRole("button", { name: /Próxima semana, / })).toHaveAttribute("data-offset", "1");
  });

  it("traz a semana clicada para o foco", () => {
    renderBoard();
    fireEvent.click(screen.getByRole("tab", { name: /Grade/ }));
    fireEvent.click(screen.getByRole("button", { name: /Semana anterior, / }));
    expect(screen.getByRole("region", { name: /Semana anterior/ })).toHaveAttribute("data-offset", "0");
    expect(screen.getByRole("button", { name: /Esta semana, / })).toHaveAttribute("data-offset", "1");
  });

  it("navega pelas setas e para nas pontas", () => {
    renderBoard();
    fireEvent.click(screen.getByRole("tab", { name: /Grade/ }));
    fireEvent.click(screen.getByRole("button", { name: "Próxima semana" }));
    expect(screen.getByRole("button", { name: "Próxima semana" })).toBeDisabled();
    expect(screen.getByRole("region", { name: /Próxima semana/ })).toHaveAttribute("data-offset", "0");
  });

  it("mostra as colunas por semana", () => {
    renderBoard();
    fireEvent.click(screen.getByRole("tab", { name: /Colunas/ }));
    expect(screen.getAllByRole("region")).toHaveLength(5);
  });

  it("mostra a semana passada e a atual em cada cartão", () => {
    renderBoard();
    fireEvent.click(screen.getByRole("tab", { name: /Cartões/ }));
    expect(screen.getAllByText("Semana passada")).toHaveLength(4);
    expect(screen.getAllByText("Esta semana")).toHaveLength(4);
  });
});
