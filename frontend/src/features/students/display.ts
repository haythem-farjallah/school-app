import type { TFunction } from "i18next";
import type { Option } from "@/form/types";
import type { Student } from "@/types/student";

// Grade levels the backend accepts. Other stored values are shown as they are.
const gradeLevelKeys: Record<string, string> = {
  KINDERGARTEN: "students.gradeLevel.kindergarten",
  ELEMENTARY: "students.gradeLevel.elementary",
  MIDDLE: "students.gradeLevel.middle",
  HIGH: "students.gradeLevel.high",
  UNIVERSITY: "students.gradeLevel.university",
};

const genderKeys: Record<string, string> = {
  M: "students.gender.male",
  F: "students.gender.female",
  O: "students.gender.other",
};

export const gradeLevelOptions: Option[] = Object.entries(gradeLevelKeys).map(([value, label]) => ({ value, label }));

export const genderOptions: Option[] = Object.entries(genderKeys).map(([value, label]) => ({ value, label }));

export function studentName(student: Pick<Student, "firstName" | "lastName">): string {
  return `${student.firstName} ${student.lastName}`.trim();
}

/** The translated grade level, the raw stored value when it is not a known level, or null when missing. */
export function gradeLevelLabel(t: TFunction, gradeLevel: string | null | undefined): string | null {
  if (!gradeLevel?.trim()) return null;
  const key = gradeLevelKeys[gradeLevel];
  return key ? t(key) : gradeLevel;
}

/** The translated gender for M/F/O, the raw stored value otherwise, or null when missing. */
export function genderLabel(t: TFunction, gender: string | null | undefined): string | null {
  if (!gender?.trim()) return null;
  const key = genderKeys[gender];
  return key ? t(key) : gender;
}

/**
 * Formats a backend LocalDate ("yyyy-MM-dd") for the given locale, or explains
 * that it is missing or invalid. The date is formatted in UTC so it never shifts
 * by a day in the viewer's time zone.
 */
export function formatStudentDate(t: TFunction, value: string | null | undefined, locale: string): string {
  if (!value?.trim()) return t("students.detail.notProvided");
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value.trim());
  const [year, month, day] = match ? match.slice(1).map(Number) : [];
  const date = match ? new Date(Date.UTC(year, month - 1, day)) : null;
  if (!date || date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) {
    return t("students.detail.invalidDate");
  }
  return new Intl.DateTimeFormat(locale, { dateStyle: "long", timeZone: "UTC" }).format(date);
}
