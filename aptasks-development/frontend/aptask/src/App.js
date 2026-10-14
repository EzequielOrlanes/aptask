import './App.css';
import React, { useCallback, useEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import Task from "./pages/Task.js";
import Home from "./pages/Home.js";
import Login from "./pages/Login.js";
import { getWeeklyTasks, getResidents, getTrashStatus, updateResidentPresence, submitWeeklyDecision } from "./services/api";

function App() {
  const [loggedUser, setLoggedUser] = useState("");
  const [weeklyTasks, setWeeklyTasks] = useState([]);
  const [residents, setResidents] = useState([]);
  const [trashStatus, setTrashStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const refreshTasks = useCallback(async () => {
    setLoading(true);
    try {
      const results = await Promise.allSettled([getWeeklyTasks(), getResidents(), getTrashStatus()]);
      const [tasksResult, residentsResult, trashResult] = results;
      if (tasksResult.status === "fulfilled") setWeeklyTasks(tasksResult.value);
      if (residentsResult.status === "fulfilled") setResidents(residentsResult.value);
      if (trashResult.status === "fulfilled") setTrashStatus(trashResult.value);
      const failed = results.some(result => result.status === "rejected");
      setError(failed ? "Algumas informações não carregaram. Confira a conexão com o backend." : "");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { refreshTasks(); }, [refreshTasks]);

  const currentTasks = weeklyTasks.map(item => ({
    ...item,
    task: item.description,
    status: item.status === "done",
    data_and_time: item.status === "done" ? "Concluída nesta semana" : "",
  }));
  const task = weeklyTasks.find(item => item.userName === loggedUser);
  const submitDecision = async done => {
    const updated = await submitWeeklyDecision(loggedUser, done);
    setWeeklyTasks(items => items.map(item => item.id === updated.id ? updated : item));
  };
  const changePresence = async isAtHome => {
    const resident = residents.find(item => item.name === loggedUser);
    if (!resident?.id) throw new Error("O morador não possui um identificador da API.");
    await updateResidentPresence(resident.id, isAtHome);
    await refreshTasks();
  };

  return (
    <Router>
      {error && <div className="app-alert" role="alert">{error}<button onClick={refreshTasks}>Tentar novamente</button></div>}
      <Routes>
        <Route path="/" element={<Home loggedUser={loggedUser} setLoggedUser={setLoggedUser} tasks={currentTasks} residents={residents} trashStatus={trashStatus} loading={loading} />} />
        <Route path="/login" element={<Login />} />
        <Route path="/user" element={loggedUser ? <Task loggedUser={loggedUser} resident={residents.find(item => item.name === loggedUser)} task={task} loading={loading} onDecision={submitDecision} onPresenceChange={changePresence} /> : <Navigate to="/" />} />
        <Route path="/home" element={<Home loggedUser={loggedUser} setLoggedUser={setLoggedUser} tasks={currentTasks} residents={residents} trashStatus={trashStatus} loading={loading} />} />
      </Routes>
    </Router>
  );
}

export default App;
