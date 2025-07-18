import type { z } from "zod";
import type { ElementType } from "react";

export type FieldType =
  | "text"
  | "password"
  | "number"
  | "checkbox"
  | "color"
  | "date"
  | "select"
  | "student-search"
  | "custom";

export interface Option {
  value: string | number;
  label: string;
}

export interface BaseField<T extends FieldType = FieldType> {
  name: string;
  type: T;
  label: string;
  placeholder?: string;
  props?: Record<string, unknown>;
  icon?: ElementType;
  /** Available options for select fields */
  options?: Option[];
  /** Custom component for custom field types */
  component?: ElementType;
}

export type FormRecipe = {
  schema: z.ZodTypeAny;
  fields: BaseField[];
  onSubmit: (values: unknown) => Promise<unknown>;
};
