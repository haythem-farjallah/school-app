import * as React from "react";
import { useFormContext } from "react-hook-form";
import { Check, ChevronsUpDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useTranslation } from "react-i18next";
import type { BaseField } from "./types";

interface SearchableSelectFieldProps {
  field: BaseField<"searchable-select">;
}

export function SearchableSelectField({ field }: SearchableSelectFieldProps) {
  const { setValue, watch, formState: { errors } } = useFormContext();
  const { t } = useTranslation();
  const [open, setOpen] = React.useState(false);
  const [searchQuery, setSearchQuery] = React.useState("");

  // Get the current value from the form
  const value = watch(field.name) as string | number | undefined;
  const err = errors[field.name]?.message as string | undefined;

  // Filter options based on search query
  const filteredOptions = React.useMemo(() => {
    if (!field.options) return [];
    
    if (!searchQuery.trim()) return field.options;
    
    const query = searchQuery.toLowerCase();
    return field.options.filter(option => {
      const label = option.label.toLowerCase();
      const searchableText = option.searchableText?.toLowerCase() || label;
      return label.includes(query) || searchableText.includes(query);
    });
  }, [field.options, searchQuery]);

  // Find the selected option
  const selectedOption = field.options?.find(option => option.value === value);

  const handleSelect = (optionValue: string | number) => {
    setValue(field.name, optionValue, { shouldValidate: true });
    setOpen(false);
    setSearchQuery("");
  };

  return (
    <div className="space-y-2">
      <label className="text-sm font-medium leading-none">
        {t(field.label)}
      </label>
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            role="combobox"
            aria-expanded={open}
            className={cn(
              "w-full justify-between h-11",
              err && "border-destructive/60 focus:border-destructive",
              !selectedOption && "text-muted-foreground"
            )}
          >
            {selectedOption ? (
              <span className="truncate">{selectedOption.label}</span>
            ) : (
              <span className="text-muted-foreground">
                {field.placeholder ? t(field.placeholder) : "Select option..."}
              </span>
            )}
            <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-full p-0" align="start">
          <Command shouldFilter={false}>
            <div className="flex items-center border-b px-3">
              <CommandInput
                placeholder={field.searchPlaceholder || "Search..."}
                value={searchQuery}
                onValueChange={setSearchQuery}
                className="flex h-10 w-full rounded-md bg-transparent py-3 text-sm outline-none placeholder:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-50"
              />
            </div>
            <CommandList>
              <CommandEmpty>
                {searchQuery.trim() ? "No options found." : "No options available."}
              </CommandEmpty>
              <CommandGroup>
                {filteredOptions.map((option) => (
                  <CommandItem
                    key={String(option.value)}
                    value={String(option.value)}
                    onSelect={() => handleSelect(option.value)}
                    className="cursor-pointer"
                  >
                    <Check
                      className={cn(
                        "mr-2 h-4 w-4",
                        value === option.value ? "opacity-100" : "opacity-0"
                      )}
                    />
                    <span className="truncate">{option.label}</span>
                  </CommandItem>
                ))}
              </CommandGroup>
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>
      {err && (
        <p className="flex items-center gap-1 text-sm text-destructive">
          <span className="h-1 w-1 rounded-full bg-destructive"></span>
          {t(err)}
        </p>
      )}
    </div>
  );
}
