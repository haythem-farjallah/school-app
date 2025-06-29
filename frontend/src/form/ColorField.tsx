import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import type { BaseField } from "./types";
import { Palette } from "lucide-react";

const colorOptions = [
  { value: "#3B82F6", label: "Blue" },
  { value: "#EF4444", label: "Red" },
  { value: "#10B981", label: "Green" },
  { value: "#F59E0B", label: "Yellow" },
  { value: "#8B5CF6", label: "Purple" },
  { value: "#EC4899", label: "Pink" },
  { value: "#6B7280", label: "Gray" },
  { value: "#14B8A6", label: "Teal" },
];

export const ColorField = ({ field }: { field: BaseField<"color"> }) => {
  const {
    watch,
    setValue,
    formState: { errors },
  } = useFormContext();
  const { t } = useTranslation();
  
  const value = watch(field.name);
  const err = errors[field.name]?.message as string | undefined;

  return (
    <div className="space-y-2">
      <label className="text-sm font-semibold text-gray-700 flex items-center space-x-2">
        <Palette className="h-4 w-4" />
        <span>{t(field.label)}</span>
      </label>
      
      <div className="grid grid-cols-4 gap-3">
        {colorOptions.map((color) => (
          <button
            key={color.value}
            type="button"
            onClick={() => setValue(field.name, color.value)}
            className={`relative h-12 w-full rounded-xl border-2 transition-all duration-200 hover:scale-105 ${
              value === color.value
                ? "border-gray-800 ring-2 ring-blue-200"
                : "border-gray-200 hover:border-gray-300"
            }`}
          >
            <div
              className="h-full w-full rounded-lg"
              style={{ backgroundColor: color.value }}
            />
            {value === color.value && (
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="h-2 w-2 bg-white rounded-full shadow-lg" />
              </div>
            )}
          </button>
        ))}
      </div>
      
      {err && (
        <p className="flex items-center gap-1 text-sm text-destructive">
          <span className="h-1 w-1 rounded-full bg-destructive"></span>
          {t(err)}
        </p>
      )}
    </div>
  );
}; 