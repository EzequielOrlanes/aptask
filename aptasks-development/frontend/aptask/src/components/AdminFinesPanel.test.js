import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import AdminFinesPanel from "./AdminFinesPanel";

const fines = [
  { id: "f1", timestamp: "2026-10-15T12:00:00", amount: 5, senderName: "Casa", receiverName: "Bia", isPaid: false, direction: "all" },
  { id: "f2", timestamp: "2026-10-14T09:30:00", amount: 10.5, senderName: "Ana", receiverName: "Caio", isPaid: true, direction: "all" },
];

describe("AdminFinesPanel", () => {
  afterEach(() => jest.restoreAllMocks());

  it("lists every fine with sender, receiver and status", () => {
    render(<AdminFinesPanel fines={fines} onDelete={jest.fn()} />);

    expect(screen.getByText("Bia · R$ 5,00")).toBeInTheDocument();
    expect(screen.getByText(/De Ana/)).toHaveTextContent("Paga");
    expect(screen.getByRole("button", { name: "Remover multa de Bia" })).toBeInTheDocument();
  });

  it("removes a fine after confirmation", async () => {
    jest.spyOn(window, "confirm").mockReturnValue(true);
    const onDelete = jest.fn().mockResolvedValue(undefined);
    render(<AdminFinesPanel fines={fines} onDelete={onDelete} />);

    fireEvent.click(screen.getByRole("button", { name: "Remover multa de Bia" }));

    await waitFor(() => expect(onDelete).toHaveBeenCalledWith("f1"));
    expect(await screen.findByText("Multa removida.")).toBeInTheDocument();
  });

  it("does nothing when the admin cancels the confirmation", () => {
    jest.spyOn(window, "confirm").mockReturnValue(false);
    const onDelete = jest.fn();
    render(<AdminFinesPanel fines={fines} onDelete={onDelete} />);

    fireEvent.click(screen.getByRole("button", { name: "Remover multa de Bia" }));

    expect(onDelete).not.toHaveBeenCalled();
  });

  it("shows the backend error when removal fails", async () => {
    jest.spyOn(window, "confirm").mockReturnValue(true);
    const error = { response: { data: { error: "Multa não encontrada" } } };
    render(<AdminFinesPanel fines={fines} onDelete={jest.fn().mockRejectedValue(error)} />);

    fireEvent.click(screen.getByRole("button", { name: "Remover multa de Bia" }));

    expect(await screen.findByText("Multa não encontrada")).toBeInTheDocument();
  });

  it("shows an empty state", () => {
    render(<AdminFinesPanel fines={[]} onDelete={jest.fn()} />);
    expect(screen.getByText("Nenhuma multa registrada.")).toBeInTheDocument();
  });
});
