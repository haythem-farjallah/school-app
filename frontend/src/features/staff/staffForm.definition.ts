import { z } from "zod";
import type { BaseField } from "@/form/types";
import { User, Mail, Phone, Calendar, Home, Building, Users2 } from "lucide-react";
import { StaffType } from "@/types/staff";

/* ---------- Zod schema ---------- */
export const staffSchema = z.object({
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
  gender: z.enum(["M", "F", "O"]).optional(),
  address: z.string().optional(),
  staffType: z.nativeEnum(StaffType, { errorMap: () => ({ message: "Please select a staff type" }) }),
  department: z.string().min(1, "Department is required").max(100, "Department must be less than 100 characters"),
});
export type StaffValues = z.infer<typeof staffSchema>;

/* ---------- Field list ---------- */
export const staffFields: BaseField[] = [
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
    placeholder: "Enter address (optional)",
    icon: Home,
  },
  {
    name: "staffType",
    type: "select",
    label: "Staff Type",
    placeholder: "Select staff type",
    icon: Users2,
    options: [
      { value: "ADMINISTRATIVE", label: "Administrative" },
      { value: "MAINTENANCE", label: "Maintenance" },
      { value: "SECURITY", label: "Security" },
      { value: "LIBRARIAN", label: "Librarian" },
      { value: "COUNSELOR", label: "Counselor" },
    ],
  },
  {
    name: "department",
    type: "select",
    label: "Department",
    placeholder: "Select department",
    icon: Building,
    options: [
      { value: "ADMINISTRATION", label: "Administration" },
      { value: "MAINTENANCE", label: "Maintenance" },
      { value: "SECURITY", label: "Security" },
      { value: "LIBRARY", label: "Library" },
      { value: "COUNSELING", label: "Counseling" },
      { value: "IT", label: "Information Technology" },
      { value: "FINANCE", label: "Finance" },
      { value: "HUMAN_RESOURCES", label: "Human Resources" },
      { value: "FACILITIES", label: "Facilities" },
      { value: "TRANSPORTATION", label: "Transportation" },
    ],
  },
]; 