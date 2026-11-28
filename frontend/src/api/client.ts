import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ApiError } from '../types/api';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

// Lưu access token trong RAM (an toàn hơn localStorage vì tránh XSS)
let accessToken: string | null = null;
let onUnauthorizedCallback: (() => void) | null = null;

export const tokenStore = {
  get: () => accessToken,
  set: (token: string | null) => {
    accessToken = token;
  },
  setUnauthorizedHandler: (handler: () => void) => {
    onUnauthorizedCallback = handler;
  },
};

export const apiClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true, // bắt buộc để gửi/nhận HttpOnly cookie zoopass_refresh
  headers: {
    'Content-Type': 'application/json',
  },
});

// Gắn Bearer token vào mọi request
apiClient.interceptors.request.use((config) => {
  if (accessToken && config.headers) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// Biến giữ promise refresh đang chạy để tránh gọi /auth/refresh 2 lần song song
let refreshPromise: Promise<string | null> | null = null;

interface RetryableRequest extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

const AUTH_SKIP_REFRESH_PATHS = [
  '/auth/login',
  '/auth/refresh',
  '/auth/logout',
  '/auth/register',
];

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiError>) => {
    const originalRequest = error.config as RetryableRequest | undefined;
    const status = error.response?.status;
    const reqUrl = originalRequest?.url ?? '';

    const shouldSkip = AUTH_SKIP_REFRESH_PATHS.some((p) => reqUrl.includes(p));

    if (status === 401 && originalRequest && !originalRequest._retry && !shouldSkip) {
      originalRequest._retry = true;

      if (!refreshPromise) {
        refreshPromise = axios
          .post<{ accessToken: string }>(
            `${BASE_URL}/auth/refresh`,
            {},
            { withCredentials: true }
          )
          .then((res) => {
            const newToken = res.data.accessToken;
            accessToken = newToken;
            return newToken;
          })
          .catch(() => {
            accessToken = null;
            onUnauthorizedCallback?.();
            return null;
          })
          .finally(() => {
            refreshPromise = null;
          });
      }

      const newToken = await refreshPromise;
      if (newToken) {
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(originalRequest);
      }
    }

    return Promise.reject(error);
  }
);

// Helper lấy ApiError gọn gàng cho các form và toast thông báo
export function getApiError(error: unknown): ApiError | null {
  if (axios.isAxiosError<ApiError>(error) && error.response?.data) {
    return error.response.data;
  }
  return null;
}