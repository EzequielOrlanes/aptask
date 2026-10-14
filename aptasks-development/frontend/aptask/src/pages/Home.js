import { Link } from "react-router-dom";
import Img from "../imagem/ap103.png";
import ResidentAvatar from "../components/ResidentAvatar";
import "../style/Home.css";

function Home({ loggedUser, setLoggedUser, tasks = [], residents = [], trashStatus, loading }) {
  const displayedResidents = residents.length ? residents : residentsFallback;
  const eligibleResidents = trashStatus?.eligibleResidents || [];
  const responsible = trashStatus?.responsible;

  return (
    <main className="home-page">
      <header className="home-header">
        <Link className="brand-lockup" to="/home" aria-label="AP103 início">
          <img src={Img} alt="AP103" />
          <span>CASA EM HARMONIA</span>
        </Link>
        <span className="header-indicator"><i /> Rotina compartilhada</span>
      </header>

      <section className="home-hero">
        <p className="eyebrow">APARTAMENTO 103 · ROTINA DA CASA</p>
        <h1>Uma casa leve começa<br />com todo mundo junto.</h1>
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

      <div className="home-bottom-grid">
        <section className="home-section compact-section">
          <div className="section-heading"><div><p className="eyebrow">RESPONSABILIDADE DO DIA</p><h2>Coleta de lixo</h2></div><span className="trash-icon">⌁</span></div>
          <div className="glass-panel trash-panel">
            <p>{responsible ? "Responsável pela coleta de hoje" : "Responsável pela coleta"}</p>
            {responsible ? <div className="trash-responsible"><ResidentAvatar resident={responsible} /><strong>{responsible.name}</strong><span className="task-status done">Escalado</span></div> : <p className="trash-empty">A escala de coleta aparecerá aqui quando a API estiver disponível.</p>}
            {eligibleResidents.length > 0 && <div className="eligible-list"><small>Elegíveis hoje</small>{eligibleResidents.map(person => <span key={person.id || person.name}><ResidentAvatar resident={person} size="small" />{person.name}</span>)}</div>}
          </div>
        </section>
        <section className="home-section compact-section">
          <div className="section-heading"><div><p className="eyebrow">RECADO RÁPIDO</p><h2>Avisos da casa</h2></div><span className="notice-icon">✳</span></div>
          <div className="glass-panel notice-panel"><span className="notice-dot" /><p>Câmeras não estão funcionando.</p></div>
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
