import { scan } from "react-scan";
/**
 * Call the scanner only in dev.  Vite replaces `import.meta.env.DEV`
 * with a compile‑time constant, so the scan() call is stripped from
 * the production bundle by dead‑code‑elimination.
 */
if (import.meta.env.DEV) {
  scan();
}
export {};         
