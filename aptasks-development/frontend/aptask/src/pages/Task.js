import React, { useState } from "react";
import { Link } from "react-router-dom";
import Img from "../imagem/ap103.png";
import ResidentAvatar from "../components/ResidentAvatar";
import FinesPanel from "../components/FinesPanel";
import { FILL_LABELS, getFillLevel } from "../components/TrashBin";
import "../style/Task.css";

function Task({ loggedUser, resident, tasks = [], trashStatus, loading, onDecision, onPresenceChange, fines = [], finesLoading = false, residents = [], onApplyFine, onMarkFinePaid }) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const recordDecision = async done => {
    setSaving(true);
    setError("");
    try { await onDecision(done.taskId, done.value); }
    catch { setError("Não foi possível salvar sua resposta. Tente novamente."); }
    finally { setSaving(false); }
  };
  const changePresence = async () => {
    setError("");
    try { await onPresenceChange(!resident.isAtHome); }
    catch { setError("Não foi possível atualizar sua presença. Tente novamente."); }
  };

  return (
    <main className="task-page">
      <header className="task-header">
        <Link className="brand-lockup" to="/home"><span className="brand-mark"><img src={Img} alt="" /></span><span>CASA EM HARMONIA</span></Link>
        <Link className="back-home" to="/home">← Início</Link>
      </header>
      <section className="task-content">
        <p className="eyebrow">SUA ROTINA · ESTA SEMANA</p>
        <h1>Olá, {loggedUser}.</h1>
        <section className="presence-panel">
          <ResidentAvatar resident={resident || { name: loggedUser }} size="large" />
          <div className="presence-copy"><strong>Você está em casa?</strong><small>Seu status ajuda a calcular prazos e verificar se você pode ficar responsável pelo lixo.</small>{resident?.presenceSync === "pending" && <small role="status">Salvo neste dispositivo; será sincronizado após 30 s sem alterações.</small>}{resident?.presenceSync === "saved" && <small role="status">Presença sincronizada.</small>}</div>
          <button type="button" className={`presence-toggle ${resident?.isAtHome ? "at-home" : "away"}`} aria-pressed={!!resident?.isAtHome} disabled={!resident?.id} onClick={changePresence}>
            <span className="presence-switch" />{resident?.isAtHome ? "Estou em casa" : resident?.id ? "Estou fora" : "Conectando…"}
          </button>
        </section>
        {getFillLevel(trashStatus) !== "EMPTY" && trashStatus?.responsible?.id === resident?.id && (
          <aside className="trash-resident-nudge" role="status">
            <strong>Cutucada da casa: a lixeira está {FILL_LABELS[getFillLevel(trashStatus)].toLowerCase()}.</strong>
            <span>Você está marcado como responsável. Quando retirar o lixo, marque “Já foi esvaziado” na tela inicial.</span>
          </aside>
        )}
        <h2>Suas tarefas desta semana</h2>
        {loading ? <p>Carregando tarefas…</p> : tasks.length ? tasks.map(task => (
          <article className="weekly-resident-task" key={task.id}>
            <div className="tarefa-morador"><p>{task.description}</p></div>
            {task.deadline && <p className="task-deadline"><span>Prazo</span><strong>{new Date(task.deadline).toLocaleString("pt-BR", { dateStyle: "medium", timeStyle: "short" })}</strong></p>}
            {task.status === "done" ? (
              <>
                <p className="task-result done">Tarefa marcada como concluída. Obrigado!</p>
                <div className="task-actions"><button type="button" className="missed-button" disabled={saving} onClick={() => recordDecision({ taskId: task.id, value: false })}>Marcar como não feita</button></div>
              </>
            ) : task.fineApplied ? (
              <p className="task-result missed">Prazo não cumprido. Multa de {new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(task.fineAmount || 0)} registrada.</p>
            ) : task.decisionRecorded ? (
              <>
                <p className="task-result pending">Resposta registrada. Se continuar pendente no prazo, a regra de multa será aplicada.</p>
                <div className="task-actions"><button type="button" className="done-button" disabled={saving} onClick={() => recordDecision({ taskId: task.id, value: true })}>Marcar como feita</button></div>
              </>
            ) : (
              <>
                <p className="task-question">Você concluiu esta tarefa?</p>
                <div className="task-actions">
                  <button type="button" className="done-button" disabled={saving} onClick={() => recordDecision({ taskId: task.id, value: true })}>Fiz</button>
                  <button type="button" className="missed-button" disabled={saving} onClick={() => recordDecision({ taskId: task.id, value: false })}>Ainda não fiz</button>
                </div>
              </>
            )}
          </article>
        )) : <p>Você não tem tarefas atribuídas nesta semana.</p>}
        {error && <p role="alert" className="task-error">{error}</p>}
        <FinesPanel
          fines={fines}
          residents={residents}
          currentResident={resident}
          loading={finesLoading}
          onApplyFine={onApplyFine}
          onMarkFinePaid={onMarkFinePaid}
        />
      <p className="week-note">A distribuição é renovada semanalmente. O responsável e o prazo são definidos pelo backend.</p>
      </section>
      <footer className="task-footer">AP103 <span>•</span> Feito para morar melhor</footer>
    </main>
  );
}

export default Task;
