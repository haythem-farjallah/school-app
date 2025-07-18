import type { BaseField } from "./types";
import { InputField } from "./InputField";
import { CheckboxField } from "./CheckboxField";
import { ColorField } from "./ColorField";
import { SelectField } from "./SelectField";
import { StudentSearchField } from "./StudentSearchField";

/* -------------------- Type guards -------------------- */
const isTextField = (f: BaseField): f is BaseField<"text"> => f.type === "text";
const isPasswordField = (f: BaseField): f is BaseField<"password"> => f.type === "password";
const isNumberField = (f: BaseField): f is BaseField<"number"> => f.type === "number";
const isDateField = (f: BaseField): f is BaseField<"date"> => f.type === "date";
const isSelectField = (f: BaseField): f is BaseField<"select"> => f.type === "select";
const isCheckboxField = (f: BaseField): f is BaseField<"checkbox"> => f.type === "checkbox";
const isColorField = (f: BaseField): f is BaseField<"color"> => f.type === "color";
const isStudentSearchField = (f: BaseField): f is BaseField<"student-search"> => f.type === "student-search";
const isCustomField = (f: BaseField): f is BaseField<"custom"> => f.type === "custom";

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
  if (isDateField(f)) {
    return <InputField key={f.name} field={f} inputType="date" />;
  }
  if (isSelectField(f)) {
    return <SelectField key={f.name} field={f} />;
  }
  if (isCheckboxField(f)) {
    return <CheckboxField key={f.name} field={f} />;
  }
  if (isColorField(f)) {
    return <ColorField key={f.name} field={f} />;
  }
  if (isStudentSearchField(f)) {
    return <StudentSearchField key={f.name} field={f} />;
  }
  if (isCustomField(f)) {
    const CustomComponent = f.component;
    return <CustomComponent key={f.name} name={f.name} label={f.label} placeholder={f.placeholder} />;
  }

  if (import.meta.env.DEV) {
    console.warn("Unknown field type:", f);
  }

  return null;
};
