import { z } from "zod";
import type { BaseField } from "@/form/types";

/* ─── Forgot Password Schema ─────────────────────────────────────────── */
export const forgotPasswordSchema = z.object({
  email: z.string().email("Please enter a valid email address"),
});
export type ForgotPasswordValues = z.infer<typeof forgotPasswordSchema>;

/* ─── Reset Password Schema ─────────────────────────────────────────── */
export const resetPasswordSchema = z.object({
  email: z.string().email("Please enter a valid email address"),
  otp: z.string().min(6, "OTP must be 6 digits").max(6, "OTP must be 6 digits"),
  newPassword: z.string().min(8, "Password must be at least 8 characters"),
  confirmPassword: z.string().min(8, "Password must be at least 8 characters"),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "Passwords don't match",
  path: ["confirmPassword"],
});
export type ResetPasswordValues = z.infer<typeof resetPasswordSchema>;

/* ─── Field Definitions ─────────────────────────────────────────────── */

export const forgotPasswordFields: BaseField[] = [
  {
    name: "email",
    type: "text",
    label: "Email Address",
    placeholder: "Enter your email address",
    props: { autoComplete: "email" },
  },
];

export const resetPasswordFields: BaseField[] = [
  {
    name: "email",
    type: "text",
    label: "Email Address",
    placeholder: "Enter your email address",
    props: { autoComplete: "email", readOnly: true },
  },
  {
    name: "otp",
    type: "text",
    label: "Verification Code",
    placeholder: "Enter 6-digit code",
    props: { 
      autoComplete: "one-time-code",
      maxLength: 6,
      pattern: "[0-9]*",
      inputMode: "numeric" as const
    },
  },
  {
    name: "newPassword",
    type: "password",
    label: "New Password",
    placeholder: "Enter new password",
    props: { autoComplete: "new-password" },
  },
  {
    name: "confirmPassword",
    type: "password",
    label: "Confirm Password",
    placeholder: "Confirm new password",
    props: { autoComplete: "new-password" },
  },
];
