// src/services/axiosInstance.js
import axios from "axios";

// Token do administrador. Fica no localStorage só para sobreviver a um recarregamento;
// a autorização real é checada pelo backend em cada rota administrativa.
export const ADMIN_TOKEN_KEY = "token";
export const ADMIN_SESSION_EXPIRED_EVENT = "ap103:admin-session-expired";

const api = axios.create({
  // REACT_APP_API_URL é gravada no build; sem ela, usa o backend no Render.
  baseURL: process.env.REACT_APP_API_URL || "https://apptasks-e6oh.onrender.com",
});

api.interceptors.request.use(
  async (config) => {
    const token = localStorage.getItem(ADMIN_TOKEN_KEY);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Token recusado (expirado ou inválido): encerra a sessão de administrador.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401 && localStorage.getItem(ADMIN_TOKEN_KEY)) {
      localStorage.removeItem(ADMIN_TOKEN_KEY);
      window.dispatchEvent(new Event(ADMIN_SESSION_EXPIRED_EVENT));
    }
    return Promise.reject(error);
  }
);

export default api;
