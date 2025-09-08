import * as React from "react";
import { Check, ChevronsUpDown, Search, Users } from "lucide-react";
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

interface StudentSearchProps {
  selectedStudents: Student[];
  onStudentsChange: (students: Student[]) => void;
  placeholder?: string;
  className?: string;
}

export function StudentSearch({ 
  selectedStudents, 
  onStudentsChange, 
  placeholder = "Search students...",
  className 
}: StudentSearchProps) {
  const [open, setOpen] = React.useState(false);
  const [searchQuery, setSearchQuery] = React.useState("");

  // Fetch students based on search query
  const { data: searchResults, isLoading } = useQuery({
    queryKey: ["students", "search", searchQuery],
    queryFn: async () => {
      if (!searchQuery.trim()) return { content: [] };
      
      const response = await http.get(`/v1/students/search`, {
        params: {
          q: searchQuery,
          page: 0,
          size: 10
        }
      });
      return response.data.data;
    },
    enabled: searchQuery.trim().length > 0,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  const handleSelectStudent = (student: Student) => {
    const isSelected = selectedStudents.some(s => s.id === student.id);
    
    if (isSelected) {
      // Remove student
      onStudentsChange(selectedStudents.filter(s => s.id !== student.id));
    } else {
      // Add student
      onStudentsChange([...selectedStudents, student]);
    }
  };

  const handleRemoveStudent = (studentId: number) => {
    onStudentsChange(selectedStudents.filter(s => s.id !== studentId));
  };

  const displayValue = selectedStudents.length > 0 
    ? `${selectedStudents.length} student(s) selected`
    : placeholder;

  return (
    <div className={cn("space-y-2", className)}>
      <div className="flex items-center space-x-2">
        <Users className="h-4 w-4 text-gray-500" />
        <label className="text-sm font-medium text-gray-700">
          Assign Children
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
                placeholder="Search by name or email..."
                value={searchQuery}
                onValueChange={setSearchQuery}
                className="border-0 focus:ring-0"
              />
            </div>
            <CommandList>
              <CommandEmpty>
                {isLoading ? "Searching..." : "No students found."}
              </CommandEmpty>
              <CommandGroup>
                {searchResults?.content?.map((student: Student) => {
                  const isSelected = selectedStudents.some(s => s.id === student.id);
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
            {selectedStudents.map((student) => (
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