import { z } from "zod";
import { EnrollmentStatus } from "@/types/enrollment";
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

  status: z
    .nativeEnum(EnrollmentStatus, {
      required_error: "Status is required",
      invalid_type_error: "Invalid status selected",
    })
    .default(EnrollmentStatus.ACTIVE),

});

export type EnrollmentFormValues = z.infer<typeof enrollmentFormDefinition>;

// Form fields definition for AutoForm
export const enrollmentFormFields: BaseField[] = [
  {
    name: "studentId",
    type: "searchable-select",
    label: "Student",
    placeholder: "Search and select a student...",
    options: [], // Will be populated dynamically
    searchable: true,
    searchPlaceholder: "Type to search students by name or email...",
  },
  {
    name: "classId",
    type: "searchable-select",
    label: "Class",
    placeholder: "Search and select a class...",
    options: [], // Will be populated dynamically
    searchable: true,
    searchPlaceholder: "Type to search classes by name...",
  },
  {
    name: "status",
    type: "select",
    label: "Initial Status",
    placeholder: "Choose status...",
    options: [
      { label: "Active", value: EnrollmentStatus.ACTIVE },
      { label: "Pending", value: EnrollmentStatus.PENDING },
      { label: "Suspended", value: EnrollmentStatus.SUSPENDED },
      { label: "Completed", value: EnrollmentStatus.COMPLETED },
      { label: "Dropped", value: EnrollmentStatus.DROPPED },
    ],
  },
];

// Form definition for updating enrollment status only
export const updateEnrollmentStatusFormDefinition = z.object({
  status: z.nativeEnum(EnrollmentStatus, {
    required_error: "Status is required",
    invalid_type_error: "Invalid status selected",
  }),
});

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

// Form definition for dropping enrollment
export const dropEnrollmentFormDefinition = z.object({
  reason: z
    .string({
      required_error: "Reason is required",
    })
    .min(3, "Reason must be at least 3 characters")
    .max(500, "Reason must be less than 500 characters"),
});

export type DropEnrollmentFormValues = z.infer<typeof dropEnrollmentFormDefinition>;

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