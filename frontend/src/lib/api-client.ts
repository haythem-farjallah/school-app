import axios, { type InternalAxiosRequestConfig } from "axios";
import { API_URL } from "./env";
import { terminateSession } from "./session";
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

// There is no token refresh endpoint: a 401 on a request sent with the current
// access token means that session is over. Requests sent without a token (login)
// and 401s that arrive after the session already ended leave the session alone,
// so concurrent 401s end it once.
api.interceptors.response.use(undefined, (error) => {
  if (axios.isAxiosError(error) && error.response?.status === 401 && sentWithCurrentToken(error.config)) {
    terminateSession();
  }
  return Promise.reject(error);
});

function sentWithCurrentToken(config: InternalAxiosRequestConfig | undefined) {
  const accessToken = token.access;
  return accessToken !== null && config?.headers.Authorization === `Bearer ${accessToken}`;
}
