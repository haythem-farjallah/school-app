import type { BaseField } from "./types";
import { InputField } from "./InputField";
import { CheckboxField } from "./CheckboxField";

// Cast explicitly to satisfy InputField type constraint
export const registry = {
  text: ({ field }: { field: BaseField<"text"> }) => <InputField field={field} />,
  password: ({ field }: { field: BaseField<"password"> }) => (
    <InputField field={field} inputType="password" />
  ),
  number: ({ field }: { field: BaseField<"number"> }) => (
    <InputField field={field} inputType="number" />
  ),
  checkbox: ({ field }: { field: BaseField<"checkbox"> }) => (
    <CheckboxField field={field} />
  ),
};
