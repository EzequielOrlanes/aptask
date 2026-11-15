import { useState } from "react";

const currency = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const dateFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium", timeStyle: "short" });

function errorMessage(error, fallback) {
  return error?.response?.data?.error || fallback;
}

function AdminFinesPanel({ fines = [], loading = false, onDelete }) {
  const [working, setWorking] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const remove = async fine => {
    if (!window.confirm(`Remover a multa de ${currency.format(Number(fine.amount))} aplicada a ${fine.receiverName}?`)) return;
    setWorking(fine.id);
    setError("");
    setNotice("");
    try {
      await onDelete(fine.id);
      setNotice("Multa removida.");
    } catch (err) {
      setError(errorMessage(err, "Não foi possível remover a multa. Tente novamente."));
    } finally {
      setWorking("");
    }
  };

  return (
    <section className="admin-task-panel glass-panel" aria-labelledby="admin-fines-title">
      <div className="admin-panel-heading">
        <div><p className="eyebrow">ÁREA ADMINISTRATIVA</p><h2 id="admin-fines-title">Multas da casa</h2></div>
        <span className="admin-badge">ADMIN</span>
      </div>

      {error && <p className="admin-feedback is-error" role="alert">{error}</p>}
      {notice && <p className="admin-feedback" role="status">{notice}</p>}

      <div className="admin-task-list">
        <div className="admin-list-title"><strong>Multas registradas</strong><span>{fines.length}</span></div>
        {loading && !fines.length ? <p className="admin-empty">Carregando multas…</p> : fines.length ? fines.map(fine => (
          <article className="admin-task-row" key={fine.id}>
            <div className="admin-task-copy">
              <strong>{fine.receiverName} · {currency.format(Number(fine.amount))}</strong>
              <small>
                De {fine.senderName}
                {fine.timestamp ? ` · ${dateFormat.format(new Date(fine.timestamp))}` : ""}
                {fine.isPaid ? " · Paga" : " · Pendente"}
              </small>
            </div>
            <div className="admin-row-actions">
              <button type="button" className="admin-delete-button" disabled={working !== ""} onClick={() => remove(fine)} aria-label={`Remover multa de ${fine.receiverName}`}>
                {working === fine.id ? "Removendo…" : "Remover"}
              </button>
            </div>
          </article>
        )) : <p className="admin-empty">Nenhuma multa registrada.</p>}
      </div>
    </section>
  );
}

export default AdminFinesPanel;
