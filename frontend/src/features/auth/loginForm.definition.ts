import { z } from "zod";
import type { BaseField } from "@/form/types";

/* ---------- Zod schema ---------- */
export const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(6),
});
export type LoginValues = z.infer<typeof loginSchema>;

/* ---------- Field list (shadcn Input/Checkbox etc.) ---------- */
export const loginFields: BaseField[] = [
  {
    name: "email",
    type: "text",
    label: "Email Address",
    placeholder: "Enter your email address",
    props: { autoComplete: "email" },
  },
  {
    name: "password",
    type: "password",
    label: "Password",
    placeholder: "Enter your password",
    props: { autoComplete: "current-password" }
  },
];
