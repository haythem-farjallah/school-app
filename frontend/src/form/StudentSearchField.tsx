import * as React from "react";
import { useFormContext } from "react-hook-form";
import { Check, ChevronsUpDown, Search } from "lucide-react";
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
import { Badge } from "@/components/ui/badge";
import { useQuery } from "@tanstack/react-query";
import { http } from "@/lib/http";
import type { Student } from "@/types/parent";
import type { BaseField } from "./types";

interface StudentSearchFieldProps {
  field: BaseField<"student-search">;
}

export function StudentSearchField({ field }: StudentSearchFieldProps) {
  const { setValue, watch } = useFormContext();
  const [open, setOpen] = React.useState(false);
  const [searchQuery, setSearchQuery] = React.useState("");

  // Get the current value from the form
  const selectedStudents = watch(field.name) || [];

  // Check if there are any students in the database
  React.useEffect(() => {
    const checkStudents = async () => {
      try {
        const response = await http.get(`/v1/students`, {
          params: { page: 0, size: 1 }
        });
        console.log("🔍 StudentSearchField - Total students check:", response);
      } catch (error) {
        console.error("🔍 StudentSearchField - Error checking students:", error);
      }
    };
    
    checkStudents();
  }, []);

  // Fetch students based on search query
  const { data: searchResults, isLoading, error } = useQuery({
    queryKey: ["students", "search", searchQuery],
    queryFn: async () => {
      if (!searchQuery.trim() || searchQuery.trim().length < 2) {
        return { content: [] };
      }
      
      console.log("🔍 StudentSearchField - Searching for:", searchQuery);
      
      try {
        const response = await http.get(`/v1/students/search`, {
          params: {
            q: searchQuery,
            page: 0,
            size: 10
          }
        });
        
        console.log("🔍 StudentSearchField - Raw response:", response);
        
        // Handle different possible response structures
        let students = [];
        if (response.data && Array.isArray(response.data)) {
          students = response.data;
        } else if (response.data && response.data.content && Array.isArray(response.data.content)) {
          students = response.data.content;
        } else if (response.data && response.data.data && Array.isArray(response.data.data)) {
          students = response.data.data;
        }
        
        console.log("🔍 StudentSearchField - Extracted students:", students);
        
        return { content: students };
      } catch (error) {
        console.error("🔍 StudentSearchField - Search error:", error);
        throw error;
      }
    },
    enabled: searchQuery.trim().length >= 2,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  const handleSelectStudent = (student: Student) => {
    const isSelected = selectedStudents.some((s: Student) => s.id === student.id);
    
    if (isSelected) {
      // Remove student
      const updatedStudents = selectedStudents.filter((s: Student) => s.id !== student.id);
      setValue(field.name, updatedStudents);
    } else {
      // Add student
      const updatedStudents = [...selectedStudents, student];
      setValue(field.name, updatedStudents);
    }
  };

  const handleRemoveStudent = (studentId: number) => {
    const updatedStudents = selectedStudents.filter((s: Student) => s.id !== studentId);
    setValue(field.name, updatedStudents);
  };

  const displayValue = selectedStudents.length > 0 
    ? `${selectedStudents.length} student(s) selected`
    : field.placeholder || "Search students...";

  const getEmptyMessage = () => {
    if (searchQuery.trim().length < 2) {
      return "Type at least 2 characters to search...";
    }
    if (isLoading) {
      return "Searching...";
    }
    if (error) {
      return "Error searching students";
    }
    return "No students found";
  };

  return (
    <div className="space-y-2">
      <div className="flex items-center space-x-2">
        {field.icon && <field.icon className="h-4 w-4 text-gray-500" />}
        <label className="text-sm font-medium text-gray-700">
          {field.label}
        </label>
      </div>
      
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            role="combobox"
            aria-expanded={open}
            className="w-full justify-between"
          >
            <span className="truncate">{displayValue}</span>
            <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-full p-0" align="start">
          <Command>
            <div className="flex items-center border-b px-3">
              <Search className="mr-2 h-4 w-4 shrink-0 opacity-50" />
              <CommandInput
                placeholder="Search by name or email (min 2 chars)..."
                value={searchQuery}
                onValueChange={setSearchQuery}
                className="border-0 focus:ring-0"
              />
            </div>
            <CommandList>
              <CommandEmpty>
                {getEmptyMessage()}
              </CommandEmpty>
              <CommandGroup>
                {searchResults?.content?.map((student: Student) => {
                  const isSelected = selectedStudents.some((s: Student) => s.id === student.id);
                  return (
                    <CommandItem
                      key={student.id}
                      value={`${student.firstName} ${student.lastName} ${student.email}`}
                      onSelect={() => handleSelectStudent(student)}
                    >
                      <Check
                        className={cn(
                          "mr-2 h-4 w-4",
                          isSelected ? "opacity-100" : "opacity-0"
                        )}
                      />
                      <div className="flex flex-col">
                        <span className="font-medium">
                          {student.firstName} {student.lastName}
                        </span>
                        <span className="text-sm text-gray-500">
                          {student.email}
                        </span>
                      </div>
                    </CommandItem>
                  );
                })}
              </CommandGroup>
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>

      {/* Selected students display */}
      {selectedStudents.length > 0 && (
        <div className="space-y-2">
          <div className="text-sm font-medium text-gray-700">
            Selected Children:
          </div>
          <div className="flex flex-wrap gap-2">
            {selectedStudents.map((student: Student) => (
              <Badge
                key={student.id}
                variant="secondary"
                className="flex items-center space-x-1"
              >
                <span>
                  {student.firstName} {student.lastName}
                </span>
                <button
                  type="button"
                  onClick={() => handleRemoveStudent(student.id)}
                  className="ml-1 rounded-full p-0.5 hover:bg-gray-300"
                >
                  <span className="sr-only">Remove</span>
                  ×
                </button>
              </Badge>
            ))}
          </div>
        </div>
      )}
    </div>
  );
} 