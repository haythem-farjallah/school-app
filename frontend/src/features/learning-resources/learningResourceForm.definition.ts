import { z } from "zod";
import type { BaseField } from "@/form/types";
import { FileText, Link, Type, Clock, Eye } from "lucide-react";
import { ResourceType } from "@/types/learning-resource";

/* ---------- Zod schema ---------- */
export const learningResourceSchema = z.object({
  title: z.string().min(1, "Title is required").max(200, "Title must be less than 200 characters"),
  description: z.string().min(1, "Description is required").max(1000, "Description must be less than 1000 characters"),
  url: z.string().url("Please enter a valid URL").optional().or(z.literal("")),
  type: z.nativeEnum(ResourceType, { errorMap: () => ({ message: "Please select a resource type" }) }),
  thumbnailUrl: z.string().url("Please enter a valid thumbnail URL").optional().or(z.literal("")),
  duration: z.number().min(0, "Duration must be positive").optional(),
  isPublic: z.boolean().default(true),
});

export type LearningResourceValues = z.infer<typeof learningResourceSchema>;

/* ---------- Field list ---------- */
export const learningResourceFields: BaseField[] = [
  {
    name: "title",
    type: "text",
    label: "Title",
    placeholder: "Enter resource title",
    icon: Type,
  },
  {
    name: "description",
    type: "text",
    label: "Description",
    placeholder: "Enter detailed description of the resource",
    icon: FileText,
  },
  {
    name: "url",
    type: "text",
    label: "URL (Optional)",
    placeholder: "Enter resource URL (for videos, links, etc.)",
    icon: Link,
  },
  {
    name: "type",
    type: "text",
    label: "Resource Type",
    placeholder: "Enter resource type (VIDEO, DOCUMENT, PRESENTATION, etc.)",
    icon: FileText,
  },
  {
    name: "thumbnailUrl",
    type: "text",
    label: "Thumbnail URL (Optional)",
    placeholder: "Enter thumbnail image URL",
    icon: Eye,
  },
  {
    name: "duration",
    type: "number",
    label: "Duration (minutes)",
    placeholder: "Enter duration in minutes (optional)",
    icon: Clock,
    props: {
      min: "0",
    },
  },
];

/* ---------- File upload schema ---------- */
export const fileUploadSchema = z.object({
  file: z.instanceof(File, { message: "Please select a file" }),
  title: z.string().min(1, "Title is required").max(200, "Title must be less than 200 characters"),
  description: z.string().min(1, "Description is required").max(1000, "Description must be less than 1000 characters"),
  type: z.nativeEnum(ResourceType, { errorMap: () => ({ message: "Please select a resource type" }) }),
  thumbnailUrl: z.string().url("Please enter a valid thumbnail URL").optional().or(z.literal("")),
  duration: z.number().min(0, "Duration must be positive").optional(),
  isPublic: z.boolean().default(true),
});

export type FileUploadValues = z.infer<typeof fileUploadSchema>; 