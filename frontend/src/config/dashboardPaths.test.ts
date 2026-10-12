import { describe, expect, it } from "vitest";
import { dashboardPathFor } from "./dashboardPaths";

describe("dashboardPathFor", () => {
  it.each([
    ["ADMIN", "/admin/dashboard"],
    ["TEACHER", "/teacher/dashboard"],
    ["STUDENT", "/student/dashboard"],
    ["PARENT", "/parent/dashboard"],
    ["STAFF", "/staff/dashboard"],
  ])("sends %s to %s", (role, path) => {
    expect(dashboardPathFor(role)).toBe(path);
  });

  it.each(["SUPERUSER", "admin", "", "constructor", "toString", null, undefined])(
    "has no dashboard for the unknown role %s",
    (role) => {
      expect(dashboardPathFor(role)).toBeNull();
    },
  );
});
