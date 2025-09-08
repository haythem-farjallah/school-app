import { z } from "zod";
import type { BaseField } from "@/form/types";

// Profile Settings Schema
export const profileSettingsSchema = z.object({
  language: z.string().min(1, "Language is required"),
  theme: z.string().min(1, "Theme is required"),
  notificationsEnabled: z.boolean(),
  darkMode: z.boolean(),
});

export type ProfileSettingsValues = z.infer<typeof profileSettingsSchema>;

export const profileSettingsFields: BaseField[] = [
  {
    name: "language",
    type: "select",
    label: "Language",
    placeholder: "Select your preferred language",
    required: true,
    options: [
      { value: "en", label: "English" },
      { value: "fr", label: "Français" },
      { value: "ar", label: "العربية" },
    ],
  },
  {
    name: "theme",
    type: "select",
    label: "Theme",
    placeholder: "Select your preferred theme",
    required: true,
    options: [
      { value: "light", label: "Light" },
      { value: "dark", label: "Dark" },
      { value: "system", label: "System" },
    ],
  },
  {
    name: "notificationsEnabled",
    type: "checkbox",
    label: "Enable Notifications",
    placeholder: "Receive notifications about important updates",
  },
  {
    name: "darkMode",
    type: "checkbox",
    label: "Dark Mode",
    placeholder: "Use dark theme for better viewing in low light",
  },
];

// Personal Information Schema - Only phone and address
export const personalInfoSchema = z.object({
  telephone: z.string().optional(),
  address: z.string().optional(),
});

export type PersonalInfoValues = z.infer<typeof personalInfoSchema>;

export const personalInfoFields: BaseField[] = [
  {
    name: "telephone",
    type: "tel",
    label: "Phone Number",
    placeholder: "Enter your phone number (optional)",
  },
  {
    name: "address",
    type: "textarea",
    label: "Address",
    placeholder: "Enter your address (optional)",
  },
];
