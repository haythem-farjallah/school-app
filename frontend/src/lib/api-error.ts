import axios from "axios";

const DEFAULT_MESSAGE = "Something went wrong. Please try again.";

/**
 * Returns the message to show the user for a failed API call. This is the only
 * place that knows the error body shapes, checked in this order:
 *
 * 1. `detail`  – RFC 7807 ProblemDetail, the target backend contract
 * 2. `message` – the current ApiErrorResponse and the rate-limit response
 * 3. `error`   – Spring Boot's default error body (e.g. "Unauthorized")
 *
 * A request that got no response (network error, timeout) returns Axios' message.
 * Anything else, including non-Axios errors and non-JSON bodies such as HTML
 * error pages, returns `fallback`.
 */
export function getApiErrorMessage(error: unknown, fallback = DEFAULT_MESSAGE): string {
  if (!axios.isAxiosError(error)) {
    return fallback;
  }
  if (!error.response) {
    return text(error.message) ?? fallback;
  }
  const body: unknown = error.response.data;
  if (!isRecord(body)) {
    return fallback;
  }
  return text(body.detail) ?? text(body.message) ?? text(body.error) ?? fallback;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function text(value: unknown): string | undefined {
  return typeof value === "string" && value.trim() !== "" ? value.trim() : undefined;
}
