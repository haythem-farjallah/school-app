import { useFormContext } from "react-hook-form";
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from "@/components/ui/select";
import { useTranslation } from "react-i18next";
import type { BaseField } from "./types";
import { cn } from "@/lib/utils";

interface Props {
  field: BaseField<"select">;
}

export function SelectField({ field }: Props) {
  const { setValue, watch, formState: { errors } } = useFormContext();
  const { t } = useTranslation();

  // value from RHF
  const value = watch(field.name) as string | number | undefined;
  const err = errors[field.name]?.message as string | undefined;

  // Check if field options contain numeric values
  const hasNumericValues = field.options?.some(opt => typeof opt.value === 'number');

  const handleValueChange = (v: string) => {
    // Convert to number if the field has numeric option values
    const finalValue = hasNumericValues ? parseInt(v) : v;
    setValue(field.name, finalValue, { shouldValidate: true });
  };

  return (
    <div className="space-y-2">
      <label className="text-sm font-medium leading-none">
        {t(field.label)}
      </label>
      <Select
        value={value ? String(value) : undefined}
        onValueChange={handleValueChange}
      >
        <SelectTrigger className={cn("h-11",
          err && "border-destructive/60 focus:border-destructive")}
        >
          <SelectValue placeholder={field.placeholder ? t(field.placeholder) : undefined} />
        </SelectTrigger>
        <SelectContent>
          {field.options?.map((opt) => (
            <SelectItem key={String(opt.value)} value={String(opt.value)}>
              {t(opt.label)}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      {err && (
        <p className="flex items-center gap-1 text-sm text-destructive">
          <span className="h-1 w-1 rounded-full bg-destructive"></span>
          {t(err)}
        </p>
      )}
    </div>
  );
} 