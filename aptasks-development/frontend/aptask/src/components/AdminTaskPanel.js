import React, { useEffect, useState } from "react";

function toLocalDateTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 16);
}

function AdminTaskPanel({ tasks = [], residents = [], onSave, onDelete }) {
  const [description, setDescription] = useState("");
  const [responsibleId, setResponsibleId] = useState("");
  const [dueAt, setDueAt] = useState("");
  const [editingId, setEditingId] = useState(null);
  const [working, setWorking] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    if (!responsibleId && residents.length) setResponsibleId(residents[0].id);
  }, [residents, responsibleId]);

  const resetForm = () => {
    setDescription("");
    setDueAt("");
    setEditingId(null);
    if (residents.length) setResponsibleId(residents[0].id);
  };

  const submit = async event => {
    event.preventDefault();
    if (!description.trim() || !responsibleId) return;
    setWorking(true);
    setError("");
    setNotice("");
    const request = {
      description: description.trim(),
      responsible_user: responsibleId,
      dueAt: dueAt ? new Date(dueAt).toISOString() : null,
    };
    try {
      await onSave(editingId, request);
      setNotice(editingId ? "Tarefa atualizada." : "Tarefa criada.");
      resetForm();
    } catch {
      setError("Não foi possível salvar. Verifique os dados e a conexão com a API.");
    } finally {
      setWorking(false);
    }
  };

  const edit = task => {
    setEditingId(task.id);
    setDescription(task.description || "");
    setResponsibleId(task.userId || residents.find(resident => resident.name === task.userName)?.id || "");
    setDueAt(toLocalDateTime(task.deadline));
    setError("");
    setNotice("");
    document.getElementById("admin-task-description")?.focus();
  };

  const remove = async task => {
    if (!window.confirm(`Apagar a tarefa “${task.description}”?`)) return;
    setWorking(true);
    setError("");
    setNotice("");
    try {
      await onDelete(task.id);
      if (editingId === task.id) resetForm();
      setNotice("Tarefa apagada.");
    } catch {
      setError("Não foi possível apagar a tarefa. Tente novamente.");
    } finally {
      setWorking(false);
    }
  };

  return (
    <section className="admin-task-panel glass-panel" aria-labelledby="admin-tasks-title">
      <div className="admin-panel-heading">
        <div><p className="eyebrow">ÁREA ADMINISTRATIVA</p><h2 id="admin-tasks-title">Gerenciar tarefas</h2></div>
        <span className="admin-badge">ADMIN</span>
      </div>

      <form className="admin-task-form" onSubmit={submit}>
        <label className="admin-field admin-field-description" htmlFor="admin-task-description">
          <span>Nome da tarefa</span>
          <input id="admin-task-description" value={description} onChange={event => setDescription(event.target.value)} placeholder="Ex.: Limpar a cozinha" required maxLength={180} />
        </label>
        <label className="admin-field" htmlFor="admin-task-responsible">
          <span>Responsável</span>
          <select id="admin-task-responsible" value={responsibleId} onChange={event => setResponsibleId(event.target.value)} required disabled={!residents.length}>
            {residents.map(resident => <option key={resident.id} value={resident.id}>{resident.name}</option>)}
          </select>
        </label>
        <label className="admin-field" htmlFor="admin-task-deadline">
          <span>Prazo <small>opcional</small></span>
          <input id="admin-task-deadline" type="datetime-local" value={dueAt} onChange={event => setDueAt(event.target.value)} />
        </label>
        <div className="admin-form-actions">
          <button className="admin-save-button" type="submit" disabled={working || !residents.length || !description.trim()}>{working ? "Salvando…" : editingId ? "Salvar alterações" : "Criar tarefa"}</button>
          {editingId && <button className="admin-cancel-button" type="button" disabled={working} onClick={resetForm}>Cancelar edição</button>}
        </div>
      </form>

      {error && <p className="admin-feedback is-error" role="alert">{error}</p>}
      {notice && <p className="admin-feedback" role="status">{notice}</p>}

      <div className="admin-task-list">
        <div className="admin-list-title"><strong>Tarefas cadastradas</strong><span>{tasks.length}</span></div>
        {tasks.length ? tasks.map(task => (
          <article className="admin-task-row" key={task.id}>
            <div className="admin-task-copy"><strong>{task.description}</strong><small>{task.userName || "Sem responsável"}{task.deadline ? ` · Prazo ${new Date(task.deadline).toLocaleDateString("pt-BR")}` : " · Sem prazo definido"}</small></div>
            <div className="admin-row-actions">
              <button type="button" disabled={working} onClick={() => edit(task)} aria-label={`Editar ${task.description}`}>Editar</button>
              <button type="button" className="admin-delete-button" disabled={working} onClick={() => remove(task)} aria-label={`Apagar ${task.description}`}>Apagar</button>
            </div>
          </article>
        )) : <p className="admin-empty">Nenhuma tarefa cadastrada.</p>}
      </div>
    </section>
  );
}

export default AdminTaskPanel;
