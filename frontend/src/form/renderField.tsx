import type { BaseField } from "./types";
import { InputField } from "./InputField";
import { CheckboxField } from "./CheckboxField";
import { ColorField } from "./ColorField";

/* -------------------- Type guards -------------------- */
const isTextField = (f: BaseField): f is BaseField<"text"> => f.type === "text";
const isPasswordField = (f: BaseField): f is BaseField<"password"> => f.type === "password";
const isNumberField = (f: BaseField): f is BaseField<"number"> => f.type === "number";
const isCheckboxField = (f: BaseField): f is BaseField<"checkbox"> => f.type === "checkbox";
const isColorField = (f: BaseField): f is BaseField<"color"> => f.type === "color";

/* -------------------- Renderer -------------------- */
export const renderField = (f: BaseField): JSX.Element | null => {
  if (isTextField(f)) {
    return <InputField key={f.name} field={f} inputType="text" />;
  }
  if (isPasswordField(f)) {
    return <InputField key={f.name} field={f} inputType="password" />;
  }
  if (isNumberField(f)) {
    return <InputField key={f.name} field={f} inputType="number" />;
  }
  if (isCheckboxField(f)) {
    return <CheckboxField key={f.name} field={f} />;
  }
  if (isColorField(f)) {
    return <ColorField key={f.name} field={f} />;
  }

  if (import.meta.env.DEV) {
    console.warn("Unknown field type:", f);
  }

  return null;
};
