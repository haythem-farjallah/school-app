import { z } from "zod";
import type { BaseField } from "@/form/types";
import { User, Mail, Phone, Calendar, Home, GraduationCap, BookOpen } from "lucide-react";
import { gradeLevelOptions, genderOptions } from "./display";

// Validation messages are translation keys; the form fields translate them.

const firstName = z
  .string({ required_error: "students.validation.firstNameRequired" })
  .trim()
  .min(1, "students.validation.firstNameRequired")
  .max(50, "students.validation.firstNameTooLong");

const lastName = z
  .string({ required_error: "students.validation.lastNameRequired" })
  .trim()
  .min(1, "students.validation.lastNameRequired")
  .max(50, "students.validation.lastNameTooLong");

const gradeLevel = z
  .string({ required_error: "students.validation.gradeLevelRequired" })
  .min(1, "students.validation.gradeLevelRequired");

const enrollmentYear = z
  .number({
    required_error: "students.validation.enrollmentYearRequired",
    invalid_type_error: "students.validation.enrollmentYearRequired",
  })
  .int("students.validation.enrollmentYearRange")
  .min(1900, "students.validation.enrollmentYearRange")
  .max(2100, "students.validation.enrollmentYearRange");

/* ---------- Create: POST /v1/students takes the full profile ---------- */

export const studentCreateSchema = z.object({
  firstName,
  lastName,
  email: z
    .string({ required_error: "students.validation.emailRequired" })
    .trim()
    .min(1, "students.validation.emailRequired")
    .email("students.validation.emailInvalid"),
  telephone: z
    .string()
    .optional()
    .refine((value) => !value?.trim() || /^[+]?[\d\s\-()]{8,20}$/.test(value.trim()), "students.validation.phoneInvalid"),
  birthday: z
    .string()
    .optional()
    .refine((value) => {
      if (!value) return true;
      const date = new Date(value);
      return !Number.isNaN(date.getTime()) && date <= new Date();
    }, "students.validation.birthdayInvalid"),
  gender: z.enum(["M", "F", "O"]).optional(),
  address: z.string().max(255, "students.validation.addressTooLong").optional(),
  gradeLevel,
  enrollmentYear,
});

export type StudentCreateValues = z.infer<typeof studentCreateSchema>;

const firstNameField: BaseField = {
  name: "firstName",
  type: "text",
  label: "students.form.firstName.label",
  placeholder: "students.form.firstName.placeholder",
  icon: User,
  props: { autoComplete: "off" },
};

const lastNameField: BaseField = {
  name: "lastName",
  type: "text",
  label: "students.form.lastName.label",
  placeholder: "students.form.lastName.placeholder",
  icon: User,
  props: { autoComplete: "off" },
};

const gradeLevelField: BaseField = {
  name: "gradeLevel",
  type: "select",
  label: "students.form.gradeLevel.label",
  placeholder: "students.form.gradeLevel.placeholder",
  icon: GraduationCap,
  options: gradeLevelOptions,
};

const enrollmentYearField: BaseField = {
  name: "enrollmentYear",
  type: "number",
  label: "students.form.enrollmentYear.label",
  placeholder: "students.form.enrollmentYear.placeholder",
  icon: BookOpen,
  // No native min/max: the browser's own validation bubble would replace the translated messages.
  props: { inputMode: "numeric" },
};

export const studentCreateFields: BaseField[] = [
  firstNameField,
  lastNameField,
  {
    name: "email",
    type: "text",
    label: "students.form.email.label",
    placeholder: "students.form.email.placeholder",
    icon: Mail,
    props: { inputMode: "email", autoComplete: "off" },
  },
  {
    name: "telephone",
    type: "text",
    label: "students.form.phone.label",
    placeholder: "students.form.phone.placeholder",
    icon: Phone,
    props: { type: "tel" },
  },
  {
    name: "birthday",
    type: "date",
    label: "students.form.birthday.label",
    icon: Calendar,
  },
  {
    name: "gender",
    type: "select",
    label: "students.form.gender.label",
    placeholder: "students.form.gender.placeholder",
    options: genderOptions,
  },
  gradeLevelField,
  enrollmentYearField,
  {
    name: "address",
    type: "text",
    label: "students.form.address.label",
    placeholder: "students.form.address.placeholder",
    icon: Home,
  },
];

/* ---------- Edit: PATCH /v1/students/{id} changes only these four fields ---------- */

export const studentEditSchema = z.object({
  firstName,
  lastName,
  gradeLevel,
  enrollmentYear,
});

export type StudentEditValues = z.infer<typeof studentEditSchema>;

export const studentEditFields: BaseField[] = [firstNameField, lastNameField, gradeLevelField, enrollmentYearField];
