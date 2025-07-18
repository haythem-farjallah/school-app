import { z } from "zod";
import type { BaseField } from "@/form/types";
import { User, Mail, Phone, Calendar, Home, GraduationCap, BookOpen } from "lucide-react";

// Utility to generate academic years like "2024-2025"
const generateAcademicYears = (span = 5): { value: string; label: string }[] => {
  const current = new Date().getFullYear();
  return Array.from({ length: span }, (_, i) => {
    const start = current + i;
    const end = start + 1;
    return { value: `${start}`, label: `${start}-${end}` };
  });
};

/* ---------- Zod schema ---------- */
export const studentSchema = z.object({
  firstName: z.string().min(1, "First name is required").max(50, "First name must be less than 50 characters"),
  lastName: z.string().min(1, "Last name is required").max(50, "Last name must be less than 50 characters"),
  email: z.string().email("Please enter a valid email address"),
  telephone: z.string().optional(),
  birthday: z.string().optional(),
  gender: z.enum(["M", "F", "O"]).optional(),
  address: z.string().optional(),
  gradeLevel: z.string().min(1, "Grade level is required").max(20, "Grade level must be less than 20 characters"),
  enrollmentYear: z.coerce.number().min(1900, "Enrollment year must be at least 1900").max(2100, "Enrollment year cannot exceed 2100").optional(),
});

export type StudentValues = z.infer<typeof studentSchema>;

/* ---------- Field list ---------- */
export const studentFields: BaseField[] = [
  {
    name: "firstName",
    type: "text",
    label: "First Name",
    placeholder: "Enter first name",
    icon: User,
  },
  {
    name: "lastName",
    type: "text",
    label: "Last Name",
    placeholder: "Enter last name",
    icon: User,
  },
  {
    name: "email",
    type: "text",
    label: "Email Address",
    placeholder: "Enter email address",
    icon: Mail,
  },
  {
    name: "telephone",
    type: "number",
    label: "Phone Number",
    placeholder: "Enter phone number (optional)",
    icon: Phone,
  },
  {
    name: "birthday",
    type: "date",
    label: "Birthday",
    placeholder: "Select date…",
    icon: Calendar,
  },
  {
    name: "gender",
    type: "select",
    label: "Gender",
    placeholder: "Select gender",
    icon: User,
    options: [
      { value: "M", label: "Male" },
      { value: "F", label: "Female" },
      { value: "O", label: "Other" },
    ],
  },
  {
    name: "address",
    type: "text",
    label: "Address",
    placeholder: "Enter complete address (optional)",
    icon: Home,
  },
  {
    name: "gradeLevel",
    type: "select",
    label: "Grade Level",
    placeholder: "Select grade level",
    icon: GraduationCap,
    options: [
      { value: "KINDERGARTEN", label: "Kindergarten" },
      { value: "ELEMENTARY", label: "Elementary" },
      { value: "MIDDLE", label: "Middle" },
      { value: "HIGH", label: "High" },
      { value: "UNIVERSITY", label: "University" },
    ],
  },
  {
    name: "enrollmentYear",
    type: "select",
    label: "Enrollment Year",
    placeholder: "Select year",
    icon: BookOpen,
    options: generateAcademicYears(6),
  },
]; 