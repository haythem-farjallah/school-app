import { z } from "zod";
import type { BaseField } from "@/form/types";
import { BookOpen, Palette, User, Clock } from "lucide-react";
import { TeacherField } from "./components/teacher-field";

/* ---------- Zod schema ---------- */
export const courseSchema = z.object({
  name: z.string().min(1, "Course name is required").max(100, "Course name must be less than 100 characters"),
  color: z.string().min(4, "Please select a color").max(7, "Invalid color format"),
  credit: z.number().min(0.1, "Credit must be at least 0.1").max(10, "Credit must be at most 10"),
  weeklyCapacity: z.number().min(1, "Weekly capacity must be at least 1").max(40, "Weekly capacity must be at most 40"),
  teacherId: z.number().min(1, "Please select a teacher").optional(),
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
    name: "credit",
    type: "number",
    label: "Credit",
    placeholder: "1.0",
    props: { 
      step: "0.1",
      min: "0.1",
      max: "10"
    },
  },
  {
    name: "weeklyCapacity",
    type: "number",
    label: "Weekly Hours",
    placeholder: "3",
    icon: Clock,
    props: {
      min: "1",
      max: "40"
    },
  },
  {
    name: "teacherId",
    type: "custom",
    label: "Assigned Teacher",
    placeholder: "Select a teacher",
    icon: User,
    component: TeacherField,
  },
]; 