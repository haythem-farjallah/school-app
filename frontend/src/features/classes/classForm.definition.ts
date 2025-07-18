import { z } from "zod";
import { FormRecipe } from "@/form/types";

export const classFormSchema = z.object({
  name: z.string().min(1, "Class name is required").max(100, "Class name must be less than 100 characters"),
  yearOfStudy: z.number().min(1, "Year of study must be at least 1").max(12, "Year of study must be less than 12"),
  maxStudents: z.number().min(1, "Maximum students must be at least 1").max(200, "Maximum students must be less than 200"),
});

export type ClassFormValues = z.infer<typeof classFormSchema>;

export const classFormDefinition: FormRecipe = {
  schema: classFormSchema,
  fields: [
    {
      name: "name",
      label: "Class Name",
      type: "text",
      placeholder: "Enter class name (e.g., Grade 10A, Computer Science 2024)",
      description: "A unique identifier for the class",
      required: true,
    },
    {
      name: "yearOfStudy",
      label: "Year of Study",
      type: "number",
      placeholder: "Enter year of study",
      description: "The academic year level (1-12)",
      required: true,
    },
    {
      name: "maxStudents",
      label: "Maximum Students",
      type: "number",
      placeholder: "Enter maximum number of students",
      description: "Maximum number of students allowed in this class",
      required: true,
    },
  ],
  onSubmit: () => {
    throw new Error("onSubmit must be implemented by the component using this form");
  },
}; 