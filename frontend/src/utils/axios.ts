import axios, { AxiosRequestConfig } from 'axios';

import { getAccessToken, setAccessToken, clearAccessToken } from './authToken';

const axiosServices = axios.create({
  baseURL: import.meta.env.VITE_APP_API_URL || 'http://localhost:8080/',
  // Send/receive the HttpOnly refresh-token cookie on cross-origin requests
  withCredentials: true
});

// ==============================|| AXIOS - FOR MOCK SERVICES ||============================== //

axiosServices.interceptors.request.use(
  async (config) => {
    const accessToken = getAccessToken();
    if (accessToken) {
      config.headers['Authorization'] = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Single-flight refresh: concurrent 401s share one /refresh call.
let refreshPromise: Promise<string | null> | null = null;

export async function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = axiosServices
      .post('/api/auth/v1/refresh')
      .then((res) => {
        const token: string | null = res.data?.data?.token ?? null;
        setAccessToken(token);
        return token;
      })
      .catch(() => {
        clearAccessToken();
        return null;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

axiosServices.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config || {};
    const status = error.response?.status;
    const url: string = original.url || '';
    const isAuthEndpoint = url.includes('/api/auth/');

    // On 401, try one transparent refresh-and-retry (except for auth endpoints).
    if (status === 401 && !original._retry && !isAuthEndpoint) {
      original._retry = true;
      const token = await refreshAccessToken();
      if (token) {
        original.headers = original.headers || {};
        original.headers['Authorization'] = `Bearer ${token}`;
        return axiosServices(original);
      }
      if (!window.location.href.includes('/login')) {
        redirectWithBasePath('/login');
      }
    }
    return Promise.reject((error.response && error.response.data) || 'Wrong Services');
  }
);

export default axiosServices;

export const fetcher = async (args: string | [string, AxiosRequestConfig]) => {
  const [url, config] = Array.isArray(args) ? args : [args];

  const res = await axiosServices.get(url, { ...config });

  return res.data;
};

export function redirectWithBasePath(path: string) {
  const basePath = import.meta.env.VITE_APP_BASE_NAME || process.env.VITE_APP_BASE_NAME || ''; // adjust for Vite, CRA, etc.
  window.location.pathname = `${basePath.replace(/\/$/, '')}${path}`;
}
