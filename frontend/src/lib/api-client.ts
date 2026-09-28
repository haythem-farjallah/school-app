import axios, { type InternalAxiosRequestConfig } from "axios";
import { API_URL } from "./env";
import { terminateSession, updateAccessToken } from "./session";
import { token } from "./token";

/**
 * HTTP client for feature API functions. It returns the normal AxiosResponse;
 * each feature API function returns `response.data` itself.
 */
export const api = axios.create({ baseURL: API_URL });

// The refresh call has its own client so a rejected refresh never re-enters the 401 handling below.
const refreshClient = axios.create({ baseURL: API_URL });

/** POST /api/auth/refresh response body (ApiSuccessResponse<RefreshTokenResponse>) */
interface RefreshEnvelope {
  status: string;
  data: { accessToken: string };
}

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retried?: boolean;
}

api.interceptors.request.use((config) => {
  const accessToken = token.access;
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

// A 401 on a request sent with the current access token means that token expired or was
// rejected: it is renewed once with the refresh token and the request is retried once. When
// there is no refresh token, the refresh token is rejected or the retry is rejected too, the
// session ends. A refresh that fails for another reason (rate limit, server error, network)
// only fails the request. Requests sent without a token (login) and 401s for a token that
// belongs to an ended or newer session leave the current session alone.
api.interceptors.response.use(undefined, async (error) => {
  if (!axios.isAxiosError(error) || error.response?.status !== 401 || !error.config) {
    return Promise.reject(error);
  }
  const config: RetriableRequestConfig = error.config;
  const sentToken = sentAccessToken(config);
  if (sentToken === null || config._retried) {
    if (sentToken !== null && sentToken === token.access) {
      terminateSession();
    }
    return Promise.reject(error);
  }

  const renewedToken = await renewedAccessToken(sentToken);
  // Retry only while the renewed token is still the session's token (no logout or new login meanwhile).
  if (renewedToken === null || renewedToken !== token.access) {
    return Promise.reject(error);
  }
  config._retried = true;
  return api(config);
});

function sentAccessToken(config: InternalAxiosRequestConfig) {
  const header = config.headers.Authorization;
  return typeof header === "string" && header.startsWith("Bearer ") ? header.slice("Bearer ".length) : null;
}

// Concurrent 401s for the same access token share one refresh request, including 401s
// that arrive after it already completed. A refresh that failed is forgotten once it settles,
// so the next request that meets a 401 may try again.
let lastRefresh: { expiredToken: string; renewed: Promise<string | null> } | undefined;

/** Resolves to the access token that replaces `expiredToken`, or null when there is none. */
function renewedAccessToken(expiredToken: string): Promise<string | null> {
  if (lastRefresh?.expiredToken === expiredToken) {
    return lastRefresh.renewed;
  }
  if (expiredToken !== token.access) {
    return Promise.resolve(null);
  }
  const refreshToken = token.refresh;
  if (!refreshToken) {
    terminateSession();
    return Promise.resolve(null);
  }
  const attempt = { expiredToken, renewed: refreshAccessToken(expiredToken, refreshToken) };
  lastRefresh = attempt;
  void attempt.renewed.then((accessToken) => {
    if (accessToken === null && lastRefresh === attempt) {
      lastRefresh = undefined;
    }
  });
  return attempt.renewed;
}

async function refreshAccessToken(expiredToken: string, refreshToken: string): Promise<string | null> {
  // The outcome only applies to the session that asked for it, not to one started meanwhile.
  const sameSession = () => token.access === expiredToken && token.refresh === refreshToken;
  try {
    const response = await refreshClient.post<RefreshEnvelope>("/auth/refresh", { refreshToken });
    const accessToken = response.data.data.accessToken;
    if (!sameSession()) {
      return null;
    }
    updateAccessToken(accessToken);
    return accessToken;
  } catch (error) {
    // Only a rejected refresh token ends the session; the session outlives a failed attempt.
    if (sameSession() && axios.isAxiosError(error) && error.response?.status === 401) {
      terminateSession();
    }
    return null;
  }
}
