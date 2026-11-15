// src/services/axiosInstance.js
import axios from "axios";

const api = axios.create({
  // REACT_APP_API_URL é gravada no build; sem ela, usa o backend no Render.
  baseURL: process.env.REACT_APP_API_URL || "https://apptasks-e6oh.onrender.com",
});

api.interceptors.request.use(
  async (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export default api;
