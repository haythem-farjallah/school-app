import { z } from "zod";
import type { BaseField } from "@/form/types";
import { User, Mail, Phone, Calendar, Home, MessageCircle, Users } from "lucide-react";

/* ---------- Zod schema ---------- */
export const parentSchema = z.object({
  firstName: z.string().min(1, "First name is required").max(50, "First name must be less than 50 characters"),
  lastName: z.string().min(1, "Last name is required").max(50, "Last name must be less than 50 characters"),
  email: z.string().email("Please enter a valid email address"),
  telephone: z.string().optional(),
  birthday: z.string().optional(),
  gender: z.string().optional(),
  address: z.string().optional(),
  preferredContactMethod: z.enum(['EMAIL', 'PHONE', 'SMS', 'WHATSAPP', 'IN_PERSON']).optional(),
  relation: z.enum(['FATHER', 'MOTHER', 'GUARDIAN', 'SIBLING', 'OTHER']).optional(),
  children: z.array(z.any()).optional(), // Array of selected students
});
export type ParentValues = z.infer<typeof parentSchema>;

/* ---------- Schema for updates (only updatable fields) ---------- */
export const parentUpdateSchema = z.object({
  telephone: z.string().optional(),
  address: z.string().optional(),
  preferredContactMethod: z.enum(['EMAIL', 'PHONE', 'SMS', 'WHATSAPP', 'IN_PERSON']).optional(),
  relation: z.enum(['FATHER', 'MOTHER', 'GUARDIAN', 'SIBLING', 'OTHER']).optional(),
  children: z.array(z.any()).optional(), // Add children assignment to edit
});
export type ParentUpdateValues = z.infer<typeof parentUpdateSchema>;

/* ---------- Field list ---------- */
export const parentFields: BaseField[] = [
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
    name: "preferredContactMethod",
    type: "select",
    label: "Preferred Contact Method",
    placeholder: "Select preferred contact method (optional)",
    icon: MessageCircle,
    options: [
      { value: "EMAIL", label: "Email" },
      { value: "PHONE", label: "Phone" },
      { value: "SMS", label: "SMS" },
      { value: "WHATSAPP", label: "WhatsApp" },
      { value: "IN_PERSON", label: "In Person" },
    ],
  },
  {
    name: "relation",
    type: "select",
    label: "Relation to Child",
    placeholder: "Select relation (optional)",
    icon: Users,
    options: [
      { value: "FATHER", label: "Father" },
      { value: "MOTHER", label: "Mother" },
      { value: "GUARDIAN", label: "Guardian" },
      { value: "SIBLING", label: "Sibling" },
      { value: "OTHER", label: "Other" },
    ],
  },
  {
    name: "children",
    type: "student-search",
    label: "Assign Children",
    placeholder: "Search and select children to assign...",
    icon: Users,
  },
];

/* ---------- Field list for updates (only updatable fields) ---------- */
export const parentUpdateFields: BaseField[] = [
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
    name: "preferredContactMethod",
    type: "select",
    label: "Preferred Contact Method",
    placeholder: "Select preferred contact method (optional)",
    icon: MessageCircle,
    options: [
      { value: "EMAIL", label: "Email" },
      { value: "PHONE", label: "Phone" },
      { value: "SMS", label: "SMS" },
      { value: "WHATSAPP", label: "WhatsApp" },
      { value: "IN_PERSON", label: "In Person" },
    ],
  },
  {
    name: "relation",
    type: "select",
    label: "Relation to Child",
    placeholder: "Select relation (optional)",
    icon: Users,
    options: [
      { value: "FATHER", label: "Father" },
      { value: "MOTHER", label: "Mother" },
      { value: "GUARDIAN", label: "Guardian" },
      { value: "SIBLING", label: "Sibling" },
      { value: "OTHER", label: "Other" },
    ],
  },
  {
    name: "children",
    type: "student-search",
    label: "Assign Children",
    placeholder: "Search and select children to assign...",
    icon: Users,
  },
]; 