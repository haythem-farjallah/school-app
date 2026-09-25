import axios from "axios";
import { API_URL } from "./env";
import { token } from "./token";

/**
 * HTTP client for feature API functions. It returns the normal AxiosResponse;
 * each feature API function returns `response.data` itself.
 */
export const api = axios.create({ baseURL: API_URL });

api.interceptors.request.use((config) => {
  const accessToken = token.access;
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// There is no token refresh endpoint: a 401 means the session is no longer valid.
api.interceptors.response.use(undefined, (error) => {
  if (axios.isAxiosError(error) && error.response?.status === 401) {
    token.clear();
  }
  return Promise.reject(error);
});
