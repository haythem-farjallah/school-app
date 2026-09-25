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

/**
 * Ends the current session, on logout or when the API rejects the access token:
 * clears the stored tokens, the in-memory auth state and the cached server data
 * of the account. RequireAuth then moves the user to the login page.
 */
export function terminateSession() {
  token.clear();
  sessionEndedHandler?.();
  queryClient.clear();
}
