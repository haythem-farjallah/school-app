import * as React from "react";
import { useFormContext } from "react-hook-form";
import { TeacherSearch } from "./teacher-search";

interface TeacherFieldProps {
  name: string;
  label?: string;
  placeholder?: string;
  disabled?: boolean;
}

export function TeacherField({ name, label, placeholder, disabled }: TeacherFieldProps) {
  const { setValue, watch } = useFormContext();
  const value = watch(name);

  const handleValueChange = (teacherId: number) => {
    setValue(name, teacherId);
  };

  return (
    <div className="space-y-2">
      {label && (
        <label className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
          {label}
        </label>
      )}
      <TeacherSearch
        value={value}
        onValueChange={handleValueChange}
        placeholder={placeholder}
        disabled={disabled}
      />
    </div>
  );
} 