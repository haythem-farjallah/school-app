import * as React from "react";
import { Check, ChevronsUpDown, User } from "lucide-react";
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
import { useTeachers } from "@/features/teachers/hooks/use-teachers";
import type { Teacher } from "@/types/teacher";

interface TeacherSearchProps {
  value?: number;
  onValueChange: (value: number) => void;
  placeholder?: string;
  disabled?: boolean;
}

export function TeacherSearch({ value, onValueChange, placeholder = "Select a teacher...", disabled = false }: TeacherSearchProps) {
  const [open, setOpen] = React.useState(false);
  const [search, setSearch] = React.useState("");

  const { data: teachersData, isLoading } = useTeachers({
    page: 0,
    size: 100, // Get more teachers for better selection
    search: search || undefined,
  });

  const teachers = teachersData?.data || [];

  const selectedTeacher = React.useMemo(() => {
    if (!teachers || !Array.isArray(teachers)) return undefined;
    return teachers.find((teacher: Teacher) => teacher.id === value);
  }, [teachers, value]);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          className="w-full justify-between"
          disabled={disabled || isLoading}
        >
          {selectedTeacher ? (
            <div className="flex items-center space-x-2">
              <User className="h-4 w-4 text-blue-600" />
              <span>
                {selectedTeacher.firstName} {selectedTeacher.lastName}
              </span>
            </div>
          ) : (
            <span className="text-muted-foreground">{placeholder}</span>
          )}
          <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-full p-0" align="start">
        <Command>
          <CommandInput 
            placeholder="Search teachers..." 
            value={search}
            onValueChange={setSearch}
          />
          <CommandList>
            <CommandEmpty>No teacher found.</CommandEmpty>
            <CommandGroup>
              {teachers && Array.isArray(teachers) && teachers.map((teacher: Teacher) => (
                <CommandItem
                  key={teacher.id}
                  value={`${teacher.firstName} ${teacher.lastName} ${teacher.email}`}
                  onSelect={() => {
                    onValueChange(teacher.id);
                    setOpen(false);
                    setSearch("");
                  }}
                >
                  <Check
                    className={cn(
                      "mr-2 h-4 w-4",
                      value === teacher.id ? "opacity-100" : "opacity-0"
                    )}
                  />
                  <div className="flex items-center space-x-2">
                    <User className="h-4 w-4 text-blue-600" />
                    <div>
                      <div className="font-medium">
                        {teacher.firstName} {teacher.lastName}
                      </div>
                      <div className="text-sm text-muted-foreground">
                        {teacher.email}
                      </div>
                    </div>
                  </div>
                </CommandItem>
              ))}
            </CommandGroup>
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
} 