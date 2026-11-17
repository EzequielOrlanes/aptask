import './App.css';
import React, { useCallback, useEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import Task from "./pages/Task.js";
import Home from "./pages/Home.js";
import Login from "./pages/Login.js";
import { getWeeklyTasks, getResidents, getTrashStatus, updateResidentPresence, submitWeeklyDecision, createTask, updateTask, deleteTask, assignTrashResponsible, reportTrashHalf, reportTrashFull, reportTrashEmptied, getFines, createFine, markFinePaid, getAdminFines, deleteFine } from "./services/api";
import { ADMIN_TOKEN_KEY, ADMIN_SESSION_EXPIRED_EVENT } from "./services/axiosInstance";
// Tema calmo e claro, carregado por último para valer em todas as telas.
import "./style/Calmo.css";

const PRESENCE_STORAGE_KEY = "ap103.pending-presence.v1";

function readPendingPresence() {
  try {
    const saved = JSON.parse(window.localStorage.getItem(PRESENCE_STORAGE_KEY) || "{}");
    return saved && typeof saved === "object" ? saved : {};
  } catch { return {}; }
}

function persistPendingPresence(pending) {
  try {
    if (Object.keys(pending).length) window.localStorage.setItem(PRESENCE_STORAGE_KEY, JSON.stringify(pending));
    else window.localStorage.removeItem(PRESENCE_STORAGE_KEY);
  } catch { /* localStorage may be unavailable or full; the in-memory update still works. */ }
}

function readAdminToken() {
  try { return window.localStorage.getItem(ADMIN_TOKEN_KEY) || null; }
  catch { return null; }
}

function App() {
  const [loggedUser, setLoggedUser] = useState("");
  const [adminToken, setAdminToken] = useState(readAdminToken);
  const [adminFines, setAdminFines] = useState([]);
  const [adminFinesLoading, setAdminFinesLoading] = useState(false);
  const [weeklyTasks, setWeeklyTasks] = useState([]);
  const [residents, setResidents] = useState([]);
  const [pendingPresence, setPendingPresence] = useState(readPendingPresence);
  const [trashStatus, setTrashStatus] = useState(null);
  const [fines, setFines] = useState([]);
  const [finesLoading, setFinesLoading] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const refreshTasks = useCallback(async () => {
    setLoading(true);
    try {
      const results = await Promise.allSettled([getWeeklyTasks(), getResidents(), getTrashStatus()]);
      const [tasksResult, residentsResult, trashResult] = results;
      if (tasksResult.status === "fulfilled") setWeeklyTasks(tasksResult.value);
      if (residentsResult.status === "fulfilled") setResidents(residentsResult.value.map(resident => {
        const pending = readPendingPresence()[resident.id];
        return pending ? { ...resident, isAtHome: pending.isAtHome, presenceSync: "pending" } : resident;
      }));
      if (trashResult.status === "fulfilled") setTrashStatus(trashResult.value);
      const failed = results.some(result => result.status === "rejected");
      setError(failed ? "Algumas informações não carregaram. Confira a conexão com o backend." : "");
    } finally {
      setLoading(false);
    }
  }, []);

  const refreshTrashStatus = useCallback(async () => {
    try { setTrashStatus(await getTrashStatus()); }
    catch { setError("Não foi possível atualizar o status do lixo."); }
  }, []);

  useEffect(() => { refreshTasks(); }, [refreshTasks]);
  useEffect(() => {
    const interval = window.setInterval(refreshTrashStatus, 30000);
    return () => window.clearInterval(interval);
  }, [refreshTrashStatus]);

  // Multas pertencem ao morador logado: carregadas ao escolher o perfil e atualizadas a cada 30 s.
  const currentResident = residents.find(item => item.name === loggedUser);
  const fineResidentId = currentResident?.id;
  const refreshFines = useCallback(async residentId => {
    if (!residentId) { setFines([]); return; }
    setFinesLoading(true);
    try { setFines(await getFines(residentId)); }
    catch { setError("Não foi possível carregar as multas. Confira a conexão com o backend."); }
    finally { setFinesLoading(false); }
  }, []);
  useEffect(() => { refreshFines(fineResidentId); }, [fineResidentId, refreshFines]);
  useEffect(() => {
    if (!fineResidentId) return undefined;
    const interval = window.setInterval(() => refreshFines(fineResidentId), 30000);
    return () => window.clearInterval(interval);
  }, [fineResidentId, refreshFines]);

  const currentTasks = weeklyTasks.map(item => ({
    ...item,
    task: item.description,
    status: item.status === "done",
    data_and_time: item.status === "done" ? "Concluída nesta semana" : "",
  }));
  const residentTasks = weeklyTasks.filter(item => item.userName === loggedUser);
  const applyManualFine = async ({ receiverId, amount }) => {
    if (!fineResidentId) throw new Error("Escolha seu perfil antes de aplicar uma multa.");
    await createFine({ sender: fineResidentId, receiver: receiverId, amount });
    await refreshFines(fineResidentId);
  };
  const payFine = async fineId => {
    await markFinePaid(fineId);
    await refreshFines(fineResidentId);
  };
  const submitDecision = async (taskId, done) => {
    const updated = await submitWeeklyDecision(taskId, done);
    setWeeklyTasks(items => items.map(item => item.id === updated.id ? updated : item));
  };
  const changePresence = isAtHome => {
    const resident = residents.find(item => item.name === loggedUser);
    if (!resident?.id) throw new Error("O morador não possui um identificador da API.");
    const pending = { ...pendingPresence, [resident.id]: { isAtHome, updatedAt: Date.now(), retryAt: Date.now() + 30000 } };
    persistPendingPresence(pending);
    setPendingPresence(pending);
    setResidents(items => items.map(item => item.id === resident.id ? { ...item, isAtHome, presenceSync: "pending" } : item));
  };

  // Presence changes are optimistic and debounced. The latest value is sent after
  // 30 seconds of inactivity, and a saved local change survives reloads.
  useEffect(() => {
    const entries = Object.entries(pendingPresence);
    if (!entries.length) return undefined;
    const nextDue = Math.min(...entries.map(([, value]) => value.retryAt || value.updatedAt + 30000));
    const timer = window.setTimeout(async () => {
      const now = Date.now();
      const due = Object.entries(pendingPresence).filter(([, value]) => (value.retryAt || value.updatedAt + 30000) <= now);
      await Promise.all(due.map(async ([residentId, value]) => {
        try {
          await updateResidentPresence(residentId, value.isAtHome);
          const latest = readPendingPresence();
          if (latest[residentId]?.updatedAt === value.updatedAt) {
            delete latest[residentId];
            persistPendingPresence(latest);
            setPendingPresence(latest);
          }
          setResidents(current => current.map(resident => resident.id === residentId ? { ...resident, isAtHome: value.isAtHome, presenceSync: "saved" } : resident));
          refreshTasks();
        } catch {
          setPendingPresence(current => {
            if (current[residentId]?.updatedAt !== value.updatedAt) return current;
            const next = { ...current, [residentId]: { ...value, retryAt: Date.now() + 30000 } };
            persistPendingPresence(next);
            return next;
          });
        }
      }));
    }, Math.max(0, nextDue - Date.now()));
    return () => window.clearTimeout(timer);
  }, [pendingPresence, refreshTasks]);
  const assignTrash = async residentId => {
    await assignTrashResponsible(residentId);
    await refreshTrashStatus();
  };
  const currentResidentId = () => {
    const resident = residents.find(item => item.name === loggedUser);
    if (!resident?.id) throw new Error("Escolha seu perfil antes de enviar o aviso.");
    return resident.id;
  };
  const notifyTrashHalf = async () => {
    await reportTrashHalf(currentResidentId());
    await refreshTrashStatus();
  };
  const notifyTrashFull = async () => {
    await reportTrashFull(currentResidentId());
    await refreshTrashStatus();
  };
  const notifyTrashEmptied = async () => {
    await reportTrashEmptied(currentResidentId());
    await refreshTrashStatus();
  };

  // Sessão de administrador: token emitido pelo backend após nome + senha.
  const acceptAdminToken = token => {
    try { window.localStorage.setItem(ADMIN_TOKEN_KEY, token); } catch { /* sessão só em memória */ }
    setAdminToken(token);
  };
  const leaveAdminSession = useCallback(() => {
    try { window.localStorage.removeItem(ADMIN_TOKEN_KEY); } catch { /* ignore */ }
    setAdminToken(null);
    setAdminFines([]);
  }, []);
  const isAdmin = Boolean(adminToken);

  useEffect(() => {
    window.addEventListener(ADMIN_SESSION_EXPIRED_EVENT, leaveAdminSession);
    return () => window.removeEventListener(ADMIN_SESSION_EXPIRED_EVENT, leaveAdminSession);
  }, [leaveAdminSession]);

  const refreshAdminFines = useCallback(async () => {
    setAdminFinesLoading(true);
    try { setAdminFines(await getAdminFines()); }
    catch { setError("Não foi possível carregar as multas para o administrador."); }
    finally { setAdminFinesLoading(false); }
  }, []);
  useEffect(() => {
    if (!isAdmin) return undefined;
    refreshAdminFines();
    const interval = window.setInterval(refreshAdminFines, 30000);
    return () => window.clearInterval(interval);
  }, [isAdmin, refreshAdminFines]);
  const removeFine = async fineId => {
    await deleteFine(fineId);
    await refreshAdminFines();
    await refreshFines(fineResidentId);
  };
  const saveTask = async (taskId, request) => {
    if (taskId) await updateTask(taskId, request);
    else await createTask(request);
    await refreshTasks();
  };
  const removeTask = async taskId => {
    await deleteTask(taskId);
    await refreshTasks();
  };

  const homeProps = {
    loggedUser, setLoggedUser, tasks: currentTasks, residents, trashStatus, loading, isAdmin,
    onSaveTask: saveTask, onDeleteTask: removeTask, onAdminLogout: leaveAdminSession,
    onAssignTrash: assignTrash, onReportTrashHalf: notifyTrashHalf, onReportTrashFull: notifyTrashFull, onReportTrashEmptied: notifyTrashEmptied,
    adminFines, adminFinesLoading, onDeleteFine: removeFine,
  };

  return (
    <Router>
      {error && <div className="app-alert" role="alert">{error}<button onClick={refreshTasks}>Tentar novamente</button></div>}
      <Routes>
        <Route path="/" element={<Home {...homeProps} />} />
        <Route path="/login" element={<Login onAuthenticated={acceptAdminToken} />} />
        <Route path="/user" element={loggedUser ? <Task loggedUser={loggedUser} resident={currentResident} tasks={residentTasks} trashStatus={trashStatus} loading={loading} onDecision={submitDecision} onPresenceChange={changePresence} fines={fines} finesLoading={finesLoading} residents={residents} onApplyFine={applyManualFine} onMarkFinePaid={payFine} /> : <Navigate to="/" />} />
        <Route path="/home" element={<Home {...homeProps} />} />
      </Routes>
    </Router>
  );
}

export default App;
