import { z } from "zod";
import type { BaseField } from "@/form/types";
import { BookOpen, Palette, User, Clock, Timer, Calendar, Split } from "lucide-react";
import { TeacherField } from "./components/teacher-field";

/* ---------- Zod schema ---------- */
export const courseSchema = z.object({
  name: z.string().min(1, "Course name is required").max(100, "Course name must be less than 100 characters"),
  color: z.string().min(4, "Please select a color").max(7, "Invalid color format"),
  credit: z.number().min(0.1, "Credit must be at least 0.1").max(10, "Credit must be at most 10"),
  weeklyCapacity: z.number().min(1, "Weekly capacity must be at least 1").max(40, "Weekly capacity must be at most 40"),
  durationPeriods: z.number().min(1, "Duration must be at least 1 period").max(4, "Duration must be at most 4 periods").default(1),
  weeklyFrequency: z.number().min(1, "Weekly frequency must be at least 1").max(6, "Weekly frequency must be at most 6").default(3),
  canSplit: z.boolean().default(false),
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
    name: "durationPeriods",
    type: "number",
    label: "Duration (Periods)",
    placeholder: "1",
    icon: Timer,
    props: {
      min: "1",
      max: "4"
    },
    description: "How many consecutive periods per session (1 = single period, 2 = double period, etc.)",
  },
  {
    name: "weeklyFrequency",
    type: "number",
    label: "Weekly Frequency",
    placeholder: "3",
    icon: Calendar,
    props: {
      min: "1",
      max: "6"
    },
    description: "How many times per week this course should be scheduled",
  },
  {
    name: "canSplit",
    type: "checkbox",
    label: "Can Split Across Days",
    icon: Split,
    description: "Allow this course to be split across different days of the week",
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