import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Img from "../imagem/ap103.png";
import ResidentAvatar from "../components/ResidentAvatar";
import TrashBin, { FILL_LABELS, getFillLevel } from "../components/TrashBin";
import AdminTaskPanel from "../components/AdminTaskPanel";
import AdminFinesPanel from "../components/AdminFinesPanel";
import "../style/Home.css";

function Home({ loggedUser, setLoggedUser, tasks = [], residents = [], trashStatus, loading, isAdmin, onSaveTask, onDeleteTask, onAdminLogout, onAssignTrash, onReportTrashHalf, onReportTrashFull, onReportTrashEmptied, adminFines = [], adminFinesLoading = false, onDeleteFine }) {
  const displayedResidents = residents.length ? residents : residentsFallback;
  const responsible = trashStatus?.responsible;
  const currentResidentId = residents.find(person => person.name === loggedUser)?.id;
  const isCurrentResponsible = Boolean(responsible?.id) && responsible.id === currentResidentId;
  const fillLevel = getFillLevel(trashStatus);
  const canReport = residents.some(person => person.name === loggedUser);
  const [selectedResponsibleId, setSelectedResponsibleId] = useState("");
  const [trashAction, setTrashAction] = useState("");
  const [trashActionError, setTrashActionError] = useState("");

  useEffect(() => {
    setSelectedResponsibleId(responsible?.id || "");
  }, [responsible?.id]);

  const runTrashAction = async (action, kind) => {
    setTrashAction(kind);
    setTrashActionError("");
    try { await action(); }
    catch { setTrashActionError("Não foi possível atualizar a informação do lixo. Tente novamente."); }
    finally { setTrashAction(""); }
  };

  return (
    <main className="home-page">
      <header className="home-header">
        <Link className="brand-lockup" to="/home" aria-label="AP103 início">
          <span className="brand-mark"><img src={Img} alt="" /></span>
        </Link>
        {isAdmin ? <button type="button" className="header-indicator admin-session-button" onClick={onAdminLogout}><i /> Administrador · Sair</button> : <Link className="header-indicator admin-access-link" to="/login">Acesso administrativo <span aria-hidden="true">↗</span></Link>}
      </header>

      <section className="home-hero">
        <p className="eyebrow">APARTAMENTO 103 · ROTINA DA CASA</p>
        <p className="hero-copy">Acompanhe as tarefas da semana e mantenha tudo em equilíbrio.</p>
      </section>

      <section className="home-section residents-section">
        <div className="section-heading">
          <div><p className="eyebrow">QUEM MORA AQUI</p><h2>Escolha seu perfil</h2></div>
          <span className="section-count">{displayedResidents.length.toString().padStart(2, "0")} moradores</span>
        </div>
        <div className="resident-grid">
          {displayedResidents.map((resident, index) => (
            <Link
              key={resident.id || resident.name}
              to="/user"
              className={`resident-card ${loggedUser === resident.name ? "is-selected" : ""}`}
              onClick={() => setLoggedUser(resident.name)}
            >
              <ResidentAvatar resident={resident} className={`avatar-${index + 1}`} />
              <span className="resident-copy"><strong>{resident.name}</strong><small>{resident.isAtHome === undefined ? resident.nickname : resident.isAtHome ? "Em casa" : "Fora de casa"}</small></span>
              <span className="resident-arrow" aria-hidden="true">↗</span>
            </Link>
          ))}
        </div>
      </section>

      <section className="home-section weekly-section">
        <div className="section-heading">
          <div><p className="eyebrow">SEMANA ATUAL</p><h2>Quadro de tarefas</h2></div>
          <span className="glass-badge">↻ Rotação semanal</span>
        </div>
        <div className="task-list glass-panel">
          {loading ? <p className="empty-state">Carregando tarefas…</p> : tasks.length ? tasks.map(item => (
            <article className="task-row" key={item.id || `${item.userName}-${item.task}`}>
              <span className={`task-check ${item.status ? "is-done" : ""}`}>{item.status ? "✓" : ""}</span>
              <div className="task-row-copy"><strong>{item.task}</strong><small>Responsável: {item.userName}</small></div>
              <span className={`task-status ${item.status ? "done" : "pending"}`}>{item.status ? "Concluída" : "Pendente"}</span>
            </article>
          )) : <p className="empty-state">As tarefas da semana aparecerão aqui quando a API estiver disponível.</p>}
        </div>
      </section>

      {isAdmin && <section className="home-section admin-section"><AdminTaskPanel tasks={tasks} residents={residents} onSave={onSaveTask} onDelete={onDeleteTask} /></section>}
      {isAdmin && <section className="home-section admin-section"><AdminFinesPanel fines={adminFines} loading={adminFinesLoading} onDelete={onDeleteFine} /></section>}

      <div className="home-bottom-grid">
        <section className="home-section compact-section">
          <div className="section-heading"><div><p className="eyebrow">ACOMPANHAMENTO DO LIXO</p><h2>Coleta de lixo</h2></div><span className="trash-icon">⌁</span></div>
          <div className="glass-panel trash-panel">
            <p>Quem está com o lixo?</p>
            {responsible && <div className="trash-responsible"><ResidentAvatar resident={responsible} /><strong>{responsible.name}</strong><span className="task-status done">Responsável</span></div>}
            <label className="trash-assignment-label" htmlFor="trash-responsible">Marcar responsável</label>
            <div className="trash-controls">
              <select id="trash-responsible" value={selectedResponsibleId} onChange={event => setSelectedResponsibleId(event.target.value)} disabled={!residents.length || trashAction !== ""}>
                <option value="">Escolha um morador</option>
                {residents.map(person => <option key={person.id} value={person.id}>{person.name}</option>)}
              </select>
              <button type="button" disabled={!selectedResponsibleId || trashAction !== ""} onClick={() => runTrashAction(() => onAssignTrash(selectedResponsibleId), "assign")}>
                {trashAction === "assign" ? "Salvando…" : "Salvar"}
              </button>
            </div>
            <div className="trash-status-block">
              <TrashBin level={fillLevel} />
              <p className="trash-level-line">
                Lixeira <strong>{FILL_LABELS[fillLevel].toLowerCase()}</strong>
                {responsible?.name ? ` · responsável: ${responsible.name}` : " · sem responsável"}
              </p>
              {fillLevel !== "EMPTY" && (
                <small className="trash-hint">
                  {isCurrentResponsible
                    ? 'Você está responsável. Quando retirar o lixo, marque "Já foi esvaziado".'
                    : `Só ${responsible?.name || "o responsável"} pode marcar como esvaziada.`}
                </small>
              )}
            </div>
            <div className="trash-controls trash-report-controls">
              <button type="button" className="trash-half-button" disabled={!canReport || trashAction !== "" || fillLevel !== "EMPTY"} onClick={() => runTrashAction(() => onReportTrashHalf(), "half")}>
                {trashAction === "half" ? "Enviando aviso…" : "Avisar meio cheia"}
              </button>
              <button type="button" className="trash-full-button" disabled={!canReport || trashAction !== "" || fillLevel === "FULL"} onClick={() => runTrashAction(() => onReportTrashFull(), "full")}>
                {trashAction === "full" ? "Enviando aviso…" : "Avisar cheia"}
              </button>
              {fillLevel !== "EMPTY" && isCurrentResponsible && <button type="button" className="trash-empty-button" disabled={trashAction !== ""} onClick={() => runTrashAction(() => onReportTrashEmptied(), "empty")}>
                {trashAction === "empty" ? "Atualizando…" : "Já foi esvaziado"}
              </button>}
            </div>
            {!loggedUser && <small className="trash-hint">Escolha seu perfil antes de enviar um aviso.</small>}
            {trashActionError && <p className="trash-action-error" role="alert">{trashActionError}</p>}
          </div>
        </section>
      </div>
      <footer className="home-footer">AP103 <span>•</span> Feito para morar melhor</footer>
    </main>
  );
}

const residentsFallback = [
  { name: "Ezequiel", nickname: "Eze", initials: "EZ" },
  { name: "Gabriela", nickname: "Gabs", initials: "GA" },
  { name: "Guilherme", nickname: "Gui", initials: "GU" },
  { name: "Isabella", nickname: "Bella", initials: "IS" },
];

export default Home;
