import { z } from "zod";
import type { BaseField } from "@/form/types";
import { BookOpen, Palette, User } from "lucide-react";

/* ---------- Zod schema ---------- */
export const courseSchema = z.object({
  name: z.string().min(1, "Course name is required").max(100, "Course name must be less than 100 characters"),
  color: z.string().min(4, "Please select a color").max(7, "Invalid color format"),
  coefficient: z.number().min(0.1, "Coefficient must be at least 0.1").max(10, "Coefficient must be at most 10"),
  teacherId: z.number().min(1, "Please select a teacher"),
});
export type CourseValues = z.infer<typeof courseSchema>;

/* ---------- Field list ---------- */
export const courseFields: BaseField[] = [
  {
    name: "name",
    type: "text",
    label: "Course Name",
    placeholder: "Enter course name (e.g., Physics, Mathematics)",
    icon: BookOpen,
  },
  {
    name: "color",
    type: "color",
    label: "Course Color",
    icon: Palette,
  },
  {
    name: "coefficient",
    type: "number",
    label: "Coefficient",
    placeholder: "1.0",
    props: { 
      step: "0.1",
      min: "0.1",
      max: "10"
    },
  },
  {
    name: "teacherId",
    type: "number",
    label: "Teacher ID",
    placeholder: "Enter teacher ID",
    icon: User,
    props: {
      min: "1"
    },
  },
]; 