import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";

import BoardPrototype from "./BoardPrototype";
import { getMonthWeeks, buildMockBoard } from "../../data/mockBoard";

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

  it("mostra a grade com a semana atual destacada", () => {
    renderBoard();
    fireEvent.click(screen.getByRole("tab", { name: /Grade/ }));
    expect(screen.getByText(/Protótipo com dados de exemplo/)).toBeInTheDocument();
    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(screen.getAllByText("Atual")).toHaveLength(1);
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
