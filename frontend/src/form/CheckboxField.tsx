import { useFormContext } from "react-hook-form";
import { Checkbox } from "@/components/ui/checkbox";
import { useTranslation } from "react-i18next";
import type { BaseField } from "./types";

export const CheckboxField = ({ field }: { field: BaseField<"checkbox"> }) => {
  const { register } = useFormContext();
  const { t } = useTranslation();

  return (
    <label className="flex items-center gap-2">
      <Checkbox id={field.name} {...register(field.name)} />
      {t(field.label)}
    </label>
  );
};
