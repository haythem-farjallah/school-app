import axios, {
    AxiosError,
    AxiosRequestHeaders,
    InternalAxiosRequestConfig,
  } from "axios";
  import { API_URL } from "./env";
  import { token } from "./token";
  import { notifyError } from "./notify";
  import { getErrorMessage } from "../utils/helpers";
  interface RetryableRequest extends InternalAxiosRequestConfig {
    _retry?: boolean;
    skipAuth?: boolean;
  }
  
  export const http = axios.create({
    baseURL: API_URL,
    withCredentials: true,
  });
  
  /* ─── Request interceptor ─────────────────────────────────────────────── */
  http.interceptors.request.use(
    (cfg) => {
      const c = cfg as RetryableRequest;
  
      if (!c.skipAuth && token.access && token.access !== "null") {
        if (!c.headers) c.headers = {} as AxiosRequestHeaders;
  
        (c.headers as AxiosRequestHeaders).Authorization = `Bearer ${token.access}`;
      }
  
      delete c.skipAuth;
      return c;
    },
    (error) => Promise.reject(error),
  );
  
/* ─── Response interceptor – unwrap + refresh token -------------------- */
http.interceptors.response.use(
    (res) => res.data,
    async (err: AxiosError) => {
      const original = err.config as RetryableRequest;
  
      if (err.response?.status === 401 && !original._retry && token.refresh) {
        original._retry = true;
        try {
          /* ---- call refresh endpoint (skipAuth) ------------------------ */
          const {
            data: { accessToken },
          } = await http.post<{ accessToken: string }>(
            "/auth/refresh-token",
            { refreshToken: token.refresh },
            { skipAuth: true } as RetryableRequest  // <- cast so skipAuth is allowed
          );
  
          /* ---- store token & replay original request ------------------- */
          token.access = accessToken;
  
          if (!original.headers) original.headers = {} as AxiosRequestHeaders;
          (original.headers as AxiosRequestHeaders).Authorization = `Bearer ${accessToken}`;
  
          return http(original);   // replay the request that failed
        } catch {
          token.clear();           // refresh failed → force logout
        }
      }
  
      notifyError(getErrorMessage(err));
      return Promise.reject(err);
    },
  );