import api from './axiosInstance';

// Contratos esperados pela interface (a regra de negócio e os cálculos ficam no backend):
// GET /tasks/weekly -> [{ id, userName, description, weekStart, deadline, status, fineApplied, fineAmount }]
// PATCH /tasks/weekly/{userName}/decision -> { done: boolean }, retorna a tarefa atualizada.
// GET /residents -> [{ id, name, avatarUrl, isAtHome }]
// PATCH /residents/{id}/presence -> { isAtHome: boolean }, retorna o morador atualizado.
// GET /trash/today -> { responsible: Resident|null, fillLevel: 'EMPTY'|'HALF'|'FULL', isFull, reportedAt, reportedBy }
// PATCH /trash/responsible -> { residentId }, retorna o status atualizado.
// POST /trash/half -> { reportedByResidentId }, registra que a lixeira está meio cheia.
// POST /trash/full -> { reportedByResidentId }, registra que a lixeira está cheia.
// POST /trash/empty -> { updatedByResidentId }, só o responsável; marca a lixeira como vazia.
// Aceita respostas diretas ou no envelope comum { data: ... }.
const unwrap = response => response.data?.data ?? response.data;

export const getWeeklyTasks = async () => unwrap(await api.get('/tasks/weekly'));
export const getResidents = async () => unwrap(await api.get('/residents'));
export const updateResidentPresence = async (residentId, isAtHome) =>
  unwrap(await api.patch(`/residents/${encodeURIComponent(residentId)}/presence`, { isAtHome }));
export const getTrashStatus = async () => unwrap(await api.get('/trash/today'));
export const assignTrashResponsible = async residentId =>
  unwrap(await api.patch('/trash/responsible', { residentId }));
export const reportTrashHalf = async reportedByResidentId =>
  unwrap(await api.post('/trash/half', { reportedByResidentId }));
export const reportTrashFull = async reportedByResidentId =>
  unwrap(await api.post('/trash/full', { reportedByResidentId }));
export const reportTrashEmptied = async updatedByResidentId =>
  unwrap(await api.post('/trash/empty', { updatedByResidentId }));
export const submitWeeklyDecision = async (taskId, done) =>
  unwrap(await api.patch(`/tasks/${encodeURIComponent(taskId)}/decision`, { done }));
export const createTask = async task => unwrap(await api.post('/tasks/create', task));
export const updateTask = async (taskId, task) =>
  unwrap(await api.put(`/tasks/${encodeURIComponent(taskId)}`, task));
export const deleteTask = async taskId =>
  unwrap(await api.delete(`/tasks/${encodeURIComponent(taskId)}`));

// Multas: GET /fines?userId=... -> [{ id, timestamp, amount, senderId, senderName, receiverId, receiverName, isPaid, direction: 'sent'|'received' }]
// POST /fines -> { sender, receiver, amount } (manual, o remetente é o morador logado)
// PATCH /fines/{id}/paid -> marca a multa como paga.
export const getFines = async residentId =>
  unwrap(await api.get('/fines', { params: { userId: residentId } }));
export const createFine = async ({ sender, receiver, amount }) =>
  unwrap(await api.post('/fines', { sender, receiver, amount }));
export const markFinePaid = async fineId =>
  unwrap(await api.patch(`/fines/${encodeURIComponent(fineId)}/paid`));

const API_URL = 'https://apptasks-e6oh.onrender.com/'; 

export const setStatusTasks = async (Name, Status) => {
  try {
    const response = await api.put(`${API_URL}tasks`, {
      Name,
      Status
    });
    return response.data;
  } catch (error) {
    throw error.response.data.error;
  }
};


export const registerUser = async (email, password) => {
  try {
    const response = await api.post(`${API_URL}/register`, {
      email,
      password
    });
    return response.data;
  } catch (error) {
    throw error.response.data.error;
  }
};

export const loginUser = async (email, password) => {
  try {
    const response = await api.post(`${API_URL}/login`, {
      email,
      password
    });
    return response.data;
  } catch (error) {
    throw error.response.data.error;
  }
};

export const logoutUser = async () => {
  const token = localStorage.getItem('token');
  try {
    await api.post(`${API_URL}/logout`, null, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    localStorage.removeItem("token"); // ou onde estiver armazenado o token
  } catch (error) {
    throw error.response.data.error;
  }
};

// Administrador: login pelo e-mail do morador Ezequiel (conferido no backend).
// POST /auth/admin/login -> { token, expiresAt }
export const loginAdmin = async email =>
  unwrap(await api.post('/auth/admin/login', { email }));
// GET /fines/admin -> todas as multas (rota administrativa)
export const getAdminFines = async () => unwrap(await api.get('/fines/admin'));
// DELETE /fines/{id} -> remove a multa (rota administrativa)
export const deleteFine = async fineId =>
  unwrap(await api.delete(`/fines/${encodeURIComponent(fineId)}`));
