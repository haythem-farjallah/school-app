import { z } from "zod";
import type { BaseField } from "@/form/types";
import { User, Mail, Phone, Calendar, Home, GraduationCap, BookOpen, Clock, Settings } from "lucide-react";

/* ---------- Zod schema ---------- */
export const teacherSchema = z.object({
  firstName: z.string().min(1, "First name is required").max(50, "First name must be less than 50 characters"),
  lastName: z.string().min(1, "Last name is required").max(50, "Last name must be less than 50 characters"),
  email: z.string().email("Please enter a valid email address"),
  telephone: z.string().optional().refine((val) => {
    if (!val || val.trim() === "") return true; // Optional field
    // Basic phone number validation - allows digits, spaces, hyphens, parentheses, and plus sign
    const phoneRegex = /^[+]?[\d\s\-()]{8,20}$/;
    return phoneRegex.test(val.trim());
  }, {
    message: "Please enter a valid phone number"
  }),
  birthday: z.string().optional(),
  gender: z.string().optional(),
  address: z.string().optional(),
  qualifications: z.string().min(1, "Qualifications are required").max(200, "Qualifications must be less than 200 characters"),
  subjectsTaught: z.string().min(1, "Subjects taught are required").max(100, "Subjects taught must be less than 100 characters"),
  availableHours: z.union([z.number(), z.string()]).transform((val) => {
    if (typeof val === "string") {
      const parsed = parseInt(val, 10);
      if (isNaN(parsed)) {
        throw new Error("Available hours must be a valid number");
      }
      return parsed;
    }
    return val;
  }).refine((val) => val >= 1 && val <= 50, {
    message: "Available hours must be between 1 and 50",
  }),
  schedulePreferences: z.string().min(1, "Schedule preferences are required").max(100, "Schedule preferences must be less than 100 characters"),
});
export type TeacherValues = z.infer<typeof teacherSchema>;

/* ---------- Schema for updates (only updatable fields) ---------- */
export const teacherUpdateSchema = z.object({
  telephone: z.string().optional().refine((val) => {
    if (!val || val.trim() === "") return true; // Optional field
    // Basic phone number validation - allows digits, spaces, hyphens, parentheses, and plus sign
    const phoneRegex = /^[+]?[\d\s\-()]{8,20}$/;
    return phoneRegex.test(val.trim());
  }, {
    message: "Please enter a valid phone number"
  }),
  address: z.string().optional(),
  qualifications: z.string().min(1, "Qualifications are required").max(200, "Qualifications must be less than 200 characters"),
  subjectsTaught: z.string().min(1, "Subjects taught are required").max(100, "Subjects taught must be less than 100 characters"),
  availableHours: z.union([z.number(), z.string()]).transform((val) => {
    if (typeof val === "string") {
      const parsed = parseInt(val, 10);
      if (isNaN(parsed)) {
        throw new Error("Available hours must be a valid number");
      }
      return parsed;
    }
    return val;
  }).refine((val) => val >= 1 && val <= 50, {
    message: "Available hours must be between 1 and 50",
  }),
  schedulePreferences: z.string().min(1, "Schedule preferences are required").max(100, "Schedule preferences must be less than 100 characters"),
});
export type TeacherUpdateValues = z.infer<typeof teacherUpdateSchema>;

/* ---------- Field list ---------- */
export const teacherFields: BaseField[] = [
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
    props: {
      type: "email"
    },
  },
  {
    name: "telephone",
    type: "text",
    label: "Phone Number",
    placeholder: "Enter phone number (optional)",
    icon: Phone,
    props: {
      type: "tel"
    },
  },
  {
    name: "birthday",
    type: "date",
    label: "Birthday",
    placeholder: "Select birthday (optional)",
    icon: Calendar,
  },
  {
    name: "gender",
    type: "select",
    label: "Gender",
    placeholder: "Select gender (optional)",
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
    placeholder: "Enter address (optional)",
    icon: Home,
  },
  {
    name: "qualifications",
    type: "text",
    label: "Qualifications",
    placeholder: "Enter qualifications (e.g., MSc Mathematics)",
    icon: GraduationCap,
  },
  {
    name: "subjectsTaught",
    type: "text",
    label: "Subjects Taught",
    placeholder: "Enter subjects taught (e.g., Math, Physics)",
    icon: BookOpen,
  },
  {
    name: "availableHours",
    type: "number",
    label: "Available Hours",
    placeholder: "Enter available hours per week",
    icon: Clock,
    props: {
      min: "1",
      max: "50"
    },
  },
  {
    name: "schedulePreferences",
    type: "text",
    label: "Schedule Preferences",
    placeholder: "Enter schedule preferences (e.g., Morning)",
    icon: Settings,
  },
];

/* ---------- Field list for updates (only updatable fields) ---------- */
export const teacherUpdateFields: BaseField[] = [
  {
    name: "telephone",
    type: "text",
    label: "Phone Number",
    placeholder: "Enter phone number (optional)",
    icon: Phone,
    props: {
      type: "tel"
    },
  },
  {
    name: "address",
    type: "text",
    label: "Address",
    placeholder: "Enter address (optional)",
    icon: Home,
  },
  {
    name: "qualifications",
    type: "text",
    label: "Qualifications",
    placeholder: "Enter qualifications (e.g., MSc Mathematics)",
    icon: GraduationCap,
  },
  {
    name: "subjectsTaught",
    type: "text",
    label: "Subjects Taught",
    placeholder: "Enter subjects taught (e.g., Math, Physics)",
    icon: BookOpen,
  },
  {
    name: "availableHours",
    type: "number",
    label: "Available Hours",
    placeholder: "Enter available hours per week",
    icon: Clock,
    props: {
      min: "1",
      max: "50"
    },
  },
  {
    name: "schedulePreferences",
    type: "text",
    label: "Schedule Preferences",
    placeholder: "Enter schedule preferences (e.g., Morning)",
    icon: Settings,
  },
]; 