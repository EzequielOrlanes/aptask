import { useState } from "react";
import Skeleton from "./Skeleton";

const currency = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const dateFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium", timeStyle: "short" });

// Aceita "5", "5,5", "5.50". Até 2 casas decimais, valor maior que zero.
const AMOUNT_PATTERN = /^\d+(?:[.,]\d{1,2})?$/;
export function parseFineAmount(text) {
  const normalized = String(text ?? "").trim();
  if (!AMOUNT_PATTERN.test(normalized)) return null;
  const value = Number(normalized.replace(",", "."));
  return value > 0 && value <= 99999999.99 ? value : null;
}

function errorMessage(error, fallback) {
  return error?.response?.data?.error || fallback;
}

function FineItem({ fine, busy, onMarkPaid }) {
  const isSent = fine.direction === "sent";
  const title = isSent ? `Você aplicou a ${fine.receiverName}` : `Aplicada por ${fine.senderName}`;
  const when = fine.timestamp ? dateFormat.format(new Date(fine.timestamp)) : "";
  return (
    <li className="fine-item">
      <div className="fine-copy">
        <strong>{title}</strong>
        <small>{when}</small>
      </div>
      <span className="fine-amount">{currency.format(Number(fine.amount))}</span>
      {fine.isPaid ? (
        <span className="task-status done">Paga</span>
      ) : (
        <button type="button" className="fine-pay-button" disabled={busy} onClick={() => onMarkPaid(fine.id)}>
          {busy ? "Salvando…" : "Marcar como paga"}
        </button>
      )}
    </li>
  );
}

function FinesPanel({ fines = [], residents = [], currentResident, loading = false, onApplyFine, onMarkFinePaid }) {
  const [receiverId, setReceiverId] = useState("");
  const [amountText, setAmountText] = useState("");
  const [applying, setApplying] = useState(false);
  const [payingId, setPayingId] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");

  const canApply = Boolean(currentResident?.id);
  const otherResidents = residents.filter(person => person.id && person.id !== currentResident?.id);
  const amount = parseFineAmount(amountText);
  const received = fines.filter(fine => fine.direction === "received");
  const sent = fines.filter(fine => fine.direction === "sent");

  const submit = async event => {
    event.preventDefault();
    if (!canApply || !receiverId || amount === null) return;
    setApplying(true);
    setError("");
    setNotice("");
    try {
      await onApplyFine({ receiverId, amount });
      setAmountText("");
      setReceiverId("");
      setNotice("Multa registrada.");
    } catch (err) {
      setError(errorMessage(err, "Não foi possível registrar a multa. Tente novamente."));
    } finally {
      setApplying(false);
    }
  };

  const markPaid = async fineId => {
    setPayingId(fineId);
    setError("");
    setNotice("");
    try {
      await onMarkFinePaid(fineId);
      setNotice("Multa marcada como paga.");
    } catch (err) {
      setError(errorMessage(err, "Não foi possível atualizar a multa. Tente novamente."));
    } finally {
      setPayingId("");
    }
  };

  return (
    <section className="fines-panel" aria-labelledby="fines-heading">
      <h2 id="fines-heading">Multas</h2>

      <form className="fine-form" onSubmit={submit}>
        <p className="fine-form-title">Registrar multa</p>
        <label htmlFor="fine-receiver">Morador</label>
        <select id="fine-receiver" value={receiverId} onChange={event => setReceiverId(event.target.value)} disabled={!canApply || applying}>
          <option value="">Escolha um morador</option>
          {otherResidents.map(person => <option key={person.id} value={person.id}>{person.name}</option>)}
        </select>
        <label htmlFor="fine-amount">Valor (R$)</label>
        <input
          id="fine-amount"
          type="text"
          inputMode="decimal"
          placeholder="5,00"
          value={amountText}
          onChange={event => setAmountText(event.target.value)}
          disabled={!canApply || applying}
          aria-invalid={amountText !== "" && amount === null}
        />
        {amountText !== "" && amount === null && <small role="alert" className="fine-hint">Use um valor maior que zero, com até 2 casas decimais.</small>}
        <button type="submit" disabled={!canApply || !receiverId || amount === null || applying}>
          {applying ? "Registrando…" : "Aplicar multa"}
        </button>
        {!canApply && <small className="fine-hint">Escolha seu perfil para aplicar multas.</small>}
      </form>

      {loading ? <Skeleton variant="row" count={2} label="Carregando multas…" /> : (
        <>
          <h3>Recebidas</h3>
          {received.length ? <ul className="fine-list">{received.map(fine => <FineItem key={fine.id} fine={fine} busy={payingId === fine.id} onMarkPaid={markPaid} />)}</ul> : <p className="empty-state">Nenhuma multa recebida.</p>}
          <h3>Aplicadas por você</h3>
          {sent.length ? <ul className="fine-list">{sent.map(fine => <FineItem key={fine.id} fine={fine} busy={payingId === fine.id} onMarkPaid={markPaid} />)}</ul> : <p className="empty-state">Você ainda não aplicou multas.</p>}
        </>
      )}

      {notice && <p role="status" className="fine-notice">{notice}</p>}
      {error && <p role="alert" className="task-error">{error}</p>}
    </section>
  );
}

export default FinesPanel;
