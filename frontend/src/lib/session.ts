import { token } from "./token";

let sessionEndedHandler: (() => void) | undefined;

/**
 * Registers how the application clears its in-memory session state. The HTTP
 * layer calls it through terminateSession() without depending on the store.
 */
export function setSessionEndedHandler(handler: () => void) {
  sessionEndedHandler = handler;
}

/** Ends the current session: clears the stored tokens and the in-memory auth state. */
export function terminateSession() {
  token.clear();
  sessionEndedHandler?.();
}
