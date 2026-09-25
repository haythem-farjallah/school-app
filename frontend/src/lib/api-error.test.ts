import { describe, expect, it } from "vitest";
import { AxiosError, AxiosHeaders, type AxiosResponse } from "axios";
import { getApiErrorMessage } from "./api-error";

function httpError(status: number, data: unknown) {
  const config = { headers: new AxiosHeaders() };
  const response: AxiosResponse = { data, status, statusText: "", headers: {}, config };
  return new AxiosError(`Request failed with status code ${status}`, "ERR_BAD_REQUEST", config, {}, response);
}

describe("getApiErrorMessage", () => {
  it("uses the ProblemDetail detail", () => {
    const error = httpError(409, {
      type: "about:blank",
      title: "Conflict",
      status: 409,
      detail: "Room 101 is already booked on Monday, period 2",
      instance: "/api/timetable/slots/7",
    });

    expect(getApiErrorMessage(error)).toBe("Room 101 is already booked on Monday, period 2");
  });

  it("prefers detail over message when a body has both", () => {
    const error = httpError(400, { detail: "Detail wins", message: "Legacy message" });

    expect(getApiErrorMessage(error)).toBe("Detail wins");
  });

  it("uses the message of the current error response", () => {
    const error = httpError(400, {
      timestamp: "2026-09-25T10:00:00",
      status: "BAD_REQUEST",
      message: "email: must be a well-formed email address",
      statusCode: 400,
      path: "/api/students",
    });

    expect(getApiErrorMessage(error)).toBe("email: must be a well-formed email address");
  });

  it("prefers message over error in the rate-limit response", () => {
    const error = httpError(429, {
      error: "Rate limit exceeded",
      message: "Too many requests. Please try again later.",
      retryAfter: 42,
    });

    expect(getApiErrorMessage(error)).toBe("Too many requests. Please try again later.");
  });

  it("uses error when the body has no detail or message", () => {
    const error = httpError(401, { timestamp: "2026-09-25T10:00:00", status: 401, error: "Unauthorized", path: "/api/me" });

    expect(getApiErrorMessage(error)).toBe("Unauthorized");
  });

  it("uses the Axios message when the request got no response", () => {
    const error = new AxiosError("Network Error", "ERR_NETWORK", { headers: new AxiosHeaders() }, {});

    expect(getApiErrorMessage(error)).toBe("Network Error");
  });

  it("returns the fallback for values that are not Axios errors", () => {
    for (const thrown of [new TypeError("x is undefined"), "boom", 42, null, undefined, { message: "not axios" }]) {
      expect(getApiErrorMessage(thrown)).toBe("Something went wrong. Please try again.");
    }
  });

  it("ignores fields that are not non-blank strings", () => {
    const error = httpError(400, { detail: { reason: "nested" }, message: ["a", "b"], error: "   " });

    expect(getApiErrorMessage(error)).toBe("Something went wrong. Please try again.");
  });

  it("does not show non-JSON bodies such as HTML error pages", () => {
    expect(getApiErrorMessage(httpError(502, "<html><body>Bad Gateway</body></html>"))).toBe(
      "Something went wrong. Please try again.",
    );
    expect(getApiErrorMessage(httpError(500, ["UNEXPECTED_ERROR"]))).toBe("Something went wrong. Please try again.");
    expect(getApiErrorMessage(httpError(500, null))).toBe("Something went wrong. Please try again.");
  });

  it("returns the supplied fallback", () => {
    expect(getApiErrorMessage(httpError(500, {}), "Failed to update settings")).toBe("Failed to update settings");
    expect(getApiErrorMessage(new Error("internal"), "Failed to update settings")).toBe("Failed to update settings");
  });
});
