import { queryClient } from "./query-client";
import { token } from "./token";

let sessionEndedHandler: (() => void) | undefined;

/**
 * Registers how the application clears its in-memory session state. The HTTP
 * layer calls it through terminateSession() without depending on the store.
 */
export function setSessionEndedHandler(handler: () => void) {
  sessionEndedHandler = handler;
}

let accessTokenUpdatedHandler: ((accessToken: string) => void) | undefined;

/**
 * Registers how the application updates its in-memory access token after a refresh,
 * without the HTTP layer depending on the store.
 */
export function setAccessTokenUpdatedHandler(handler: (accessToken: string) => void) {
  accessTokenUpdatedHandler = handler;
}

/**
 * Stores the access token issued by a refresh. The refresh token and the user stay the same.
 */
export function updateAccessToken(accessToken: string) {
  token.access = accessToken;
  accessTokenUpdatedHandler?.(accessToken);
}

/**
 * Ends the current session, on logout or when the access token can no longer be renewed:
 * clears the stored tokens, the in-memory auth state and the cached server data
 * of the account. RequireAuth then moves the user to the login page.
 */
export function terminateSession() {
  token.clear();
  sessionEndedHandler?.();
  queryClient.clear();
}
