import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Login from "./Login";
import { loginAdmin } from "../services/api";

jest.mock("../services/api", () => ({ loginAdmin: jest.fn() }));

const ADMIN_EMAIL = "ezequiel@example.test";

function renderLogin(onAuthenticated = jest.fn()) {
  render(<MemoryRouter><Login onAuthenticated={onAuthenticated} /></MemoryRouter>);
  return onAuthenticated;
}

describe("admin Login", () => {
  beforeEach(() => loginAdmin.mockReset());

  it("asks only for the email", () => {
    renderLogin();
    expect(screen.getByLabelText("Email:")).toBeInTheDocument();
    expect(screen.queryByLabelText("Senha:")).not.toBeInTheDocument();
  });

  it("sends the email to the backend and stores the returned token", async () => {
    loginAdmin.mockResolvedValue({ token: "tok.123", expiresAt: "2026-11-22T12:00:00Z" });
    const onAuthenticated = renderLogin();

    fireEvent.change(screen.getByLabelText("Email:"), { target: { value: `  ${ADMIN_EMAIL} ` } });
    fireEvent.click(screen.getByRole("button", { name: "Entrar" }));

    await waitFor(() => expect(onAuthenticated).toHaveBeenCalledWith("tok.123"));
    expect(loginAdmin).toHaveBeenCalledWith(ADMIN_EMAIL);
  });

  it("shows the backend message when the email is rejected", async () => {
    loginAdmin.mockRejectedValue({ response: { data: { error: "E-mail inválido para o administrador." } } });
    const onAuthenticated = renderLogin();

    fireEvent.change(screen.getByLabelText("Email:"), { target: { value: "outra@example.test" } });
    fireEvent.click(screen.getByRole("button", { name: "Entrar" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("E-mail inválido para o administrador.");
    expect(onAuthenticated).not.toHaveBeenCalled();
  });
});
