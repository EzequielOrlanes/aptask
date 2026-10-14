import api from './axiosInstance';

// Contratos esperados pela interface (a regra de negócio e os cálculos ficam no backend):
// GET /tasks/weekly -> [{ id, userName, description, weekStart, deadline, status, fineApplied, fineAmount }]
// PATCH /tasks/weekly/{userName}/decision -> { done: boolean }, retorna a tarefa atualizada.
// GET /residents -> [{ id, name, avatarUrl, isAtHome }]
// PATCH /residents/{id}/presence -> { isAtHome: boolean }, retorna o morador atualizado.
// GET /trash/today -> { date, responsible: Resident|null, eligibleResidents: Resident[] }
// Aceita respostas diretas ou no envelope comum { data: ... }.
const unwrap = response => response.data?.data ?? response.data;

export const getWeeklyTasks = async () => unwrap(await api.get('/tasks/weekly'));
export const getResidents = async () => unwrap(await api.get('/residents'));
export const updateResidentPresence = async (residentId, isAtHome) =>
  unwrap(await api.patch(`/residents/${encodeURIComponent(residentId)}/presence`, { isAtHome }));
export const getTrashStatus = async () => unwrap(await api.get('/trash/today'));
export const submitWeeklyDecision = async (userName, done) =>
  unwrap(await api.patch(`/tasks/weekly/${encodeURIComponent(userName)}/decision`, { done }));

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

export const AvisosSaveToDB = async (avisoData) => {
  try {
    const response = await api.post(
      `${API_URL}/avisos`,
       avisoData, 
      {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
    });
      return response.data;
    } catch (error) {
      throw error.response.data.error;
    }

}; 
