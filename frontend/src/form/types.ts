import type { z } from "zod";
import type { ElementType } from "react";

export type FieldType = "text" | "checkbox" | "password" | "number" | "color";

export interface BaseField<T extends FieldType = FieldType> {
  name: string;
  type: T;
  label: string;
  placeholder?: string;
  props?: Record<string, unknown>;
  icon?: ElementType;
}

export type FormRecipe = {
  schema: z.ZodTypeAny;
  fields: BaseField[];
  onSubmit: (values: unknown) => Promise<unknown>;
};
