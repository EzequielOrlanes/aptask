import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";

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

  it("avisa que o prazo foi prorrogado e a rotação está adiada", () => {
    renderBoard({ board: { ...response, extendedUntil: "2026-10-22T21:00:00Z" } });
    expect(screen.getByRole("status")).toHaveTextContent("Prazo da semana prorrogado até");
    expect(screen.getByRole("status")).toHaveTextContent("rotação acontece depois");
  });

  it("não mostra aviso de prorrogação quando o prazo não foi prorrogado", () => {
    renderBoard();
    expect(screen.queryByText(/prorrogado/)).not.toBeInTheDocument();
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

describe("editar o prazo pela data da semana", () => {
  const futureValue = () => {
    const date = new Date(Date.now() + 2 * 24 * 3600 * 1000);
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 16);
  };
  const pastValue = () => {
    const date = new Date(Date.now() - 2 * 24 * 3600 * 1000);
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 16);
  };
  const renderAdmin = onExtendWeek => render(
    <WeeklyBoard board={response} residents={[ANA, BIA]} ready canEdit onExtendWeek={onExtendWeek} />
  );
  const pick = value => fireEvent.change(screen.getByLabelText("Novo prazo"), { target: { value } });

  it("não mostra a data clicável para quem não é administrador", () => {
    render(<WeeklyBoard board={response} residents={[ANA, BIA]} ready onExtendWeek={jest.fn()} />);
    expect(screen.queryByRole("button", { name: /Editar o prazo/ })).not.toBeInTheDocument();
  });

  it("pergunta se pode salvar e envia o prazo da semana atual", async () => {
    const onExtendWeek = jest.fn().mockResolvedValue({ finesCancelled: 2, finesKeptPaid: 0 });
    renderAdmin(onExtendWeek);

    fireEvent.click(screen.getByRole("button", { name: /Editar o prazo da esta semana/ }));
    expect(screen.getByRole("dialog")).toHaveTextContent("Salvar o prazo até");
    pick(futureValue());
    expect(onExtendWeek).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() => expect(onExtendWeek).toHaveBeenCalledTimes(1));
    expect(onExtendWeek.mock.calls[0][0]).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/);
    expect(onExtendWeek.mock.calls[0][1]).toBe("current");
    expect(await screen.findByText(/2 multa\(s\) cancelada\(s\)/)).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("reabre a semana anterior quando ela está em foco", async () => {
    const onExtendWeek = jest.fn().mockResolvedValue({});
    renderAdmin(onExtendWeek);

    fireEvent.click(screen.getByRole("button", { name: /Semana anterior, / }));
    fireEvent.click(screen.getByRole("button", { name: /Editar o prazo da semana anterior/ }));
    expect(screen.getByRole("dialog")).toHaveTextContent("volta a ser a atual");
    pick(futureValue());
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    await waitFor(() => expect(onExtendWeek).toHaveBeenCalledTimes(1));
    expect(onExtendWeek.mock.calls[0][1]).toBe("previous");
  });

  it("não deixa salvar uma data no passado", () => {
    const onExtendWeek = jest.fn();
    renderAdmin(onExtendWeek);

    fireEvent.click(screen.getByRole("button", { name: /Editar o prazo da esta semana/ }));
    pick(pastValue());

    expect(screen.getByRole("alert")).toHaveTextContent("no futuro");
    expect(screen.getByRole("button", { name: "Salvar" })).toBeDisabled();
  });

  it("cancelar fecha o popup sem salvar", () => {
    const onExtendWeek = jest.fn();
    renderAdmin(onExtendWeek);

    fireEvent.click(screen.getByRole("button", { name: /Editar o prazo da esta semana/ }));
    fireEvent.click(screen.getByRole("button", { name: "Cancelar" }));

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(onExtendWeek).not.toHaveBeenCalled();
  });

  it("mostra o erro e mantém o popup aberto quando o backend falha", async () => {
    const onExtendWeek = jest.fn().mockRejectedValue(new Error("falhou"));
    renderAdmin(onExtendWeek);

    fireEvent.click(screen.getByRole("button", { name: /Editar o prazo da esta semana/ }));
    pick(futureValue());
    fireEvent.click(screen.getByRole("button", { name: "Salvar" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível salvar");
    expect(screen.getByRole("dialog")).toBeInTheDocument();
  });

  it("não oferece edição na semana seguinte", () => {
    renderAdmin(jest.fn());
    fireEvent.click(screen.getByRole("button", { name: /Próxima semana, / }));
    expect(screen.queryByRole("button", { name: /Editar o prazo da próxima semana/ })).not.toBeInTheDocument();
  });
});
