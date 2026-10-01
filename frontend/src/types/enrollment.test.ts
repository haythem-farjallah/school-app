import { describe, expect, it } from "vitest";
import {
  EnrollmentStatus,
  MANUAL_ENROLLMENT_STATUSES,
  getEnrollmentStatusColor,
  getEnrollmentStatusLabel,
  isTerminalEnrollmentStatus,
} from "./enrollment";
import {
  enrollmentFormDefinition,
  updateEnrollmentStatusFormDefinition,
  updateEnrollmentStatusFormFields,
} from "@/features/enrollments/enrollmentForm.definition";

describe("enrollment statuses", () => {
  it("are exactly the four canonical lifecycle statuses", () => {
    expect(Object.values(EnrollmentStatus)).toEqual(["ACTIVE", "COMPLETED", "TRANSFERRED", "WITHDRAWN"]);
  });

  it("have a label and a colour each", () => {
    for (const status of Object.values(EnrollmentStatus)) {
      expect(getEnrollmentStatusLabel(status)).not.toBe(status.toString());
      expect(getEnrollmentStatusColor(status)).toMatch(/^bg-/);
    }
  });

  it("treat everything except ACTIVE as terminal", () => {
    expect(isTerminalEnrollmentStatus(EnrollmentStatus.ACTIVE)).toBe(false);
    expect(isTerminalEnrollmentStatus(EnrollmentStatus.COMPLETED)).toBe(true);
    expect(isTerminalEnrollmentStatus(EnrollmentStatus.TRANSFERRED)).toBe(true);
    expect(isTerminalEnrollmentStatus(EnrollmentStatus.WITHDRAWN)).toBe(true);
  });
});

describe("enrollment forms", () => {
  it("create an enrollment from a student and a class only", () => {
    expect(Object.keys(enrollmentFormDefinition.shape)).toEqual(["studentId", "classId"]);
    expect(enrollmentFormDefinition.safeParse({ studentId: 1, classId: 2 }).success).toBe(true);
  });

  it("only let the status be completed or withdrawn by hand", () => {
    expect(MANUAL_ENROLLMENT_STATUSES).toEqual([EnrollmentStatus.COMPLETED, EnrollmentStatus.WITHDRAWN]);
    expect(updateEnrollmentStatusFormFields[0].options?.map((option) => option.value)).toEqual([
      EnrollmentStatus.COMPLETED,
      EnrollmentStatus.WITHDRAWN,
    ]);
    for (const status of [EnrollmentStatus.COMPLETED, EnrollmentStatus.WITHDRAWN]) {
      expect(updateEnrollmentStatusFormDefinition.safeParse({ status }).success).toBe(true);
    }
    for (const status of [EnrollmentStatus.ACTIVE, EnrollmentStatus.TRANSFERRED, "PENDING", "DROPPED", "SUSPENDED"]) {
      expect(updateEnrollmentStatusFormDefinition.safeParse({ status }).success).toBe(false);
    }
  });
});
