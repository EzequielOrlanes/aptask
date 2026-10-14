import React, { useState } from "react";
import { Link } from "react-router-dom";
import Img from "../imagem/ap103.png";
import ResidentAvatar from "../components/ResidentAvatar";
import "../style/Task.css";

function Task({ loggedUser, resident, task, loading, onDecision, onPresenceChange }) {
  const [saving, setSaving] = useState(false);
  const [savingPresence, setSavingPresence] = useState(false);
  const [error, setError] = useState("");
  const recordDecision = async done => {
    setSaving(true);
    setError("");
    try { await onDecision(done); }
    catch { setError("Não foi possível salvar sua resposta. Tente novamente."); }
    finally { setSaving(false); }
  };
  const changePresence = async () => {
    setSavingPresence(true);
    setError("");
    try { await onPresenceChange(!resident.isAtHome); }
    catch { setError("Não foi possível atualizar sua presença. Tente novamente."); }
    finally { setSavingPresence(false); }
  };

  return (
    <main className="task-page">
      <header className="task-header">
        <Link className="brand-lockup" to="/home"><img src={Img} alt="AP103" /><span>CASA EM HARMONIA</span></Link>
        <Link className="back-home" to="/home">← Início</Link>
      </header>
      <section className="task-content">
        <p className="eyebrow">SUA ROTINA · ESTA SEMANA</p>
        <h1>Olá, {loggedUser}.</h1>
        <section className="presence-panel">
          <ResidentAvatar resident={resident || { name: loggedUser }} size="large" />
          <div className="presence-copy"><strong>Você está em casa?</strong><small>Seu status ajuda o sistema a calcular prazos e a escala do lixo.</small></div>
          <button type="button" className={`presence-toggle ${resident?.isAtHome ? "at-home" : "away"}`} aria-pressed={!!resident?.isAtHome} disabled={!resident?.id || savingPresence} onClick={changePresence}>
            <span className="presence-switch" />{savingPresence ? "Salvando…" : resident?.isAtHome ? "Estou em casa" : resident?.id ? "Estou fora" : "Conectando…"}
          </button>
        </section>
        <h2>Sua tarefa desta semana</h2>
        {loading ? <p>Carregando tarefa…</p> : task ? <div className="tarefa-morador"><p>{task.description}</p></div> : <p>Você não tem tarefa atribuída nesta semana.</p>}
        {task?.deadline && <p className="task-deadline"><span>Prazo informado pelo backend</span><strong>{new Date(task.deadline).toLocaleString("pt-BR", { dateStyle: "medium", timeStyle: "short" })}</strong></p>}
        <h2>Você fez a tarefa?</h2>
        <div className="task-actions">
          <button type="button" className="done-button" disabled={!task || loading || saving || !!task?.status} onClick={() => recordDecision(true)}>Fiz</button>
          <button type="button" className="missed-button" disabled={!task || loading || saving || !!task?.status} onClick={() => recordDecision(false)}>Não fiz</button>
        </div>
        {task?.status && <p className={`task-result ${task.status}`}>{task.status === "done" ? "Tarefa marcada como concluída. Obrigado!" : task.fineApplied ? `Tarefa não concluída. Multa de ${new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(task.fineAmount)} registrada.` : "Tarefa não concluída. O backend registrou que não há multa para esta tarefa."}</p>}
        {error && <p role="alert" className="task-error">{error}</p>}
        <p className="week-note">A tarefa e a confirmação são renovadas a cada segunda-feira.</p>
      </section>
      <footer className="task-footer">AP103 <span>•</span> Feito para morar melhor</footer>
    </main>
  );
}

export default Task;
