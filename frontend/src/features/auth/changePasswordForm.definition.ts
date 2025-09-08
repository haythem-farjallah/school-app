import { z } from "zod";
import type { BaseField } from "@/form/types";

export const changePasswordSchema = z.object({
  oldPassword: z.string().min(1, "Current password is required"),
  newPassword: z.string().min(6, "New password must be at least 6 characters"),
  confirmPassword: z.string().min(1, "Please confirm your new password"),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "Passwords don't match",
  path: ["confirmPassword"],
});

export type ChangePasswordValues = z.infer<typeof changePasswordSchema>;

export const changePasswordFields: BaseField[] = [
  {
    name: "oldPassword",
    type: "password",
    label: "Current Password",
    placeholder: "Enter your current password",
    required: true,
  },
  {
    name: "newPassword", 
    type: "password",
    label: "New Password",
    placeholder: "Enter your new password",
    required: true,
  },
  {
    name: "confirmPassword",
    type: "password", 
    label: "Confirm New Password",
    placeholder: "Confirm your new password",
    required: true,
  },
];
