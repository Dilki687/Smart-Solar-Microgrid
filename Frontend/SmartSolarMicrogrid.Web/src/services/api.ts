import axios from "axios";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: API Client
 * Author: Dilki
 * Description: Configures the Axios client and automatically attaches
 *              the authenticated JWT to protected API requests.
 */

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
  (config) => {
    // Read the current JWT from browser storage.
    const token = localStorage.getItem("smartSolarToken");

    // Attach the token to protected API requests.
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

export default api;
