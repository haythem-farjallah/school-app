import { z } from "zod";
import { EnrollmentStatus, MANUAL_ENROLLMENT_STATUSES } from "@/types/enrollment";
import type { BaseField } from "@/form/types";

export const enrollmentFormDefinition = z.object({
  studentId: z
    .number({
      required_error: "Student is required",
      invalid_type_error: "Student must be selected",
    })
    .min(1, "Student is required"),
  
  classId: z
    .number({
      required_error: "Class is required", 
      invalid_type_error: "Class must be selected",
    })
    .min(1, "Class is required"),
});

export type EnrollmentFormValues = z.infer<typeof enrollmentFormDefinition>;

// Form fields definition for AutoForm
export const enrollmentFormFields: BaseField[] = [
  {
    name: "studentId",
    type: "select",
    label: "Student",
    placeholder: "Choose a student...",
    options: [], // Will be populated dynamically
  },
  {
    name: "classId",
    type: "select",
    label: "Class",
    placeholder: "Choose a class...",
    options: [], // Will be populated dynamically
  },
];

// Form definition for ending an active enrollment by hand (TRANSFERRED comes only from a transfer)
export const updateEnrollmentStatusFormDefinition = z.object({
  status: z.enum(MANUAL_ENROLLMENT_STATUSES, {
    required_error: "Status is required",
    invalid_type_error: "Invalid status selected",
  }),
});

export const updateEnrollmentStatusFormFields: BaseField[] = [
  {
    name: "status",
    type: "select",
    label: "New Status",
    placeholder: "Choose status...",
    options: [
      { label: "Completed", value: EnrollmentStatus.COMPLETED },
      { label: "Withdrawn", value: EnrollmentStatus.WITHDRAWN },
    ],
  },
];

export type UpdateEnrollmentStatusFormValues = z.infer<typeof updateEnrollmentStatusFormDefinition>;

// Form definition for transferring student
export const transferStudentFormDefinition = z.object({
  newClassId: z
    .number({
      required_error: "New class is required",
      invalid_type_error: "Class must be selected", 
    })
    .min(1, "New class is required"),
});

export type TransferStudentFormValues = z.infer<typeof transferStudentFormDefinition>;

// Form definition for withdrawing enrollment
export const withdrawEnrollmentFormDefinition = z.object({
  reason: z
    .string({
      required_error: "Reason is required",
    })
    .min(3, "Reason must be at least 3 characters")
    .max(500, "Reason must be less than 500 characters"),
});

export type WithdrawEnrollmentFormValues = z.infer<typeof withdrawEnrollmentFormDefinition>;

// Form definition for bulk enrollment
export const bulkEnrollFormDefinition = z.object({
  classId: z
    .number({
      required_error: "Class is required",
      invalid_type_error: "Class must be selected",
    })
    .min(1, "Class is required"),

  studentIds: z
    .array(z.number())
    .min(1, "At least one student must be selected")
    .max(50, "Cannot enroll more than 50 students at once"),
});

export type BulkEnrollFormValues = z.infer<typeof bulkEnrollFormDefinition>; 