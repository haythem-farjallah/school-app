import { z } from "zod";
import type { BaseField } from "@/form/types";
import { User, Mail, Phone, Calendar, Home, MessageCircle } from "lucide-react";

/* ---------- Zod schema ---------- */
export const parentSchema = z.object({
  firstName: z.string().min(1, "First name is required").max(50, "First name must be less than 50 characters"),
  lastName: z.string().min(1, "Last name is required").max(50, "Last name must be less than 50 characters"),
  email: z.string().email("Please enter a valid email address"),
  telephone: z.string().optional(),
  birthday: z.string().optional(),
  gender: z.string().optional(),
  address: z.string().optional(),
  preferredContactMethod: z.string().optional(),
});

export type ParentValues = z.infer<typeof parentSchema>;

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
  },
  {
    name: "telephone",
    type: "text",
    label: "Phone Number",
    placeholder: "Enter phone number (optional)",
    icon: Phone,
  },
  {
    name: "birthday",
    type: "text",
    label: "Birthday",
    placeholder: "Enter birthday (YYYY-MM-DD) (optional)",
    icon: Calendar,
  },
  {
    name: "gender",
    type: "text",
    label: "Gender",
    placeholder: "Enter gender (M/F/O) (optional)",
    icon: User,
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
    type: "text",
    label: "Preferred Contact Method",
    placeholder: "Enter preferred contact method (e.g., email, phone, sms) (optional)",
    icon: MessageCircle,
  },
]; 