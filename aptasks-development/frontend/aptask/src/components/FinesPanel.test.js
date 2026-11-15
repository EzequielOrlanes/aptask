import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import FinesPanel, { parseFineAmount } from "./FinesPanel";

const residents = [
  { id: "r-ana", name: "Ana" },
  { id: "r-bia", name: "Bia" },
  { id: "r-caio", name: "Caio" },
];
const currentResident = residents[0];

const fines = [
  { id: "f1", timestamp: "2026-10-15T12:00:00", amount: 5, senderId: "casa", senderName: "Casa", receiverId: "r-ana", receiverName: "Ana", isPaid: false, direction: "received" },
  { id: "f2", timestamp: "2026-10-14T09:30:00", amount: 10.5, senderId: "r-ana", senderName: "Ana", receiverId: "r-bia", receiverName: "Bia", isPaid: true, direction: "sent" },
];

function renderPanel(props = {}) {
  const handlers = {
    onApplyFine: jest.fn().mockResolvedValue(undefined),
    onMarkFinePaid: jest.fn().mockResolvedValue(undefined),
  };
  render(<FinesPanel fines={fines} residents={residents} currentResident={currentResident} {...handlers} {...props} />);
  return handlers;
}

describe("parseFineAmount", () => {
  it("accepts comma or dot decimals with up to two places", () => {
    expect(parseFineAmount("5")).toBe(5);
    expect(parseFineAmount("5,5")).toBe(5.5);
    expect(parseFineAmount("12.50")).toBe(12.5);
  });

  it("rejects zero, negatives, too many decimals and text", () => {
    ["0", "0,00", "-3", "1.234", "abc", "", "1e3"].forEach(text => {
      expect(parseFineAmount(text)).toBeNull();
    });
  });
});

describe("FinesPanel", () => {
  it("separates received fines from fines applied by the resident", () => {
    renderPanel();

    expect(screen.getByRole("heading", { name: "Recebidas" })).toBeInTheDocument();
    expect(screen.getByText("Aplicada por Casa")).toBeInTheDocument();
    expect(screen.getByText("Você aplicou a Bia")).toBeInTheDocument();
    expect(screen.getByText("Paga")).toBeInTheDocument();
  });

  it("marks an unpaid fine as paid through the callback", async () => {
    const { onMarkFinePaid } = renderPanel();

    fireEvent.click(screen.getByRole("button", { name: "Marcar como paga" }));

    await waitFor(() => expect(onMarkFinePaid).toHaveBeenCalledWith("f1"));
    expect(await screen.findByText("Multa marcada como paga.")).toBeInTheDocument();
  });

  it("keeps the apply button disabled until a resident and a valid amount are chosen", () => {
    renderPanel();
    const button = screen.getByRole("button", { name: "Aplicar multa" });

    expect(button).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Morador"), { target: { value: "r-bia" } });
    expect(button).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Valor (R$)"), { target: { value: "0" } });
    expect(button).toBeDisabled();
    expect(screen.getByRole("alert")).toHaveTextContent("maior que zero");

    fireEvent.change(screen.getByLabelText("Valor (R$)"), { target: { value: "7,5" } });
    expect(button).toBeEnabled();
  });

  it("does not offer the resident themselves as the receiver", () => {
    renderPanel();

    const options = screen.getAllByRole("option").map(option => option.textContent);
    expect(options).toContain("Bia");
    expect(options).not.toContain("Ana");
  });

  it("submits the chosen receiver and numeric amount, then clears the form", async () => {
    const { onApplyFine } = renderPanel();

    fireEvent.change(screen.getByLabelText("Morador"), { target: { value: "r-caio" } });
    fireEvent.change(screen.getByLabelText("Valor (R$)"), { target: { value: "2,5" } });
    fireEvent.click(screen.getByRole("button", { name: "Aplicar multa" }));

    await waitFor(() => expect(onApplyFine).toHaveBeenCalledWith({ receiverId: "r-caio", amount: 2.5 }));
    expect(await screen.findByText("Multa registrada.")).toBeInTheDocument();
    expect(screen.getByLabelText("Valor (R$)")).toHaveValue("");
  });

  it("shows the backend error message when applying a fine fails", async () => {
    const error = { response: { data: { error: "O destinatário da multa não foi encontrado." } } };
    renderPanel({ onApplyFine: jest.fn().mockRejectedValue(error) });

    fireEvent.change(screen.getByLabelText("Morador"), { target: { value: "r-bia" } });
    fireEvent.change(screen.getByLabelText("Valor (R$)"), { target: { value: "5" } });
    fireEvent.click(screen.getByRole("button", { name: "Aplicar multa" }));

    expect(await screen.findByText("O destinatário da multa não foi encontrado.")).toBeInTheDocument();
  });

  it("asks for a profile before applying fines", () => {
    renderPanel({ currentResident: undefined });

    expect(screen.getByText("Escolha seu perfil para aplicar multas.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Aplicar multa" })).toBeDisabled();
  });
});
