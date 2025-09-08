import * as React from "react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Filter, Save, Trash2, Star } from "lucide-react";
import { useQueryState } from "nuqs";
import { getFiltersStateParser } from "@/lib/parsers";
import type { ExtendedColumnFilter } from "@/types/data-table";
import type { Table } from "@tanstack/react-table";

/**
 * Filter preset definition
 */
export interface FilterPreset {
  id: string;
  name: string;
  description?: string;
  filters: ExtendedColumnFilter<any>[];
  isDefault?: boolean;
}

/**
 * Common filter presets for different entity types
 */
export const COMMON_PRESETS: Record<string, FilterPreset[]> = {
  teachers: [
    {
      id: "active-teachers",
      name: "Active Teachers",
      description: "Show only active teachers",
      filters: [
        {
          id: "status",
          value: "ACTIVE",
          variant: "select",
          operator: "eq",
          filterId: "preset-active"
        }
      ],
      isDefault: true
    },
    {
      id: "math-teachers",
      name: "Math Teachers",
      description: "Teachers who teach mathematics",
      filters: [
        {
          id: "subjectsTaught",
          value: "math",
          variant: "text",
          operator: "like",
          filterId: "preset-math"
        }
      ]
    },
    {
      id: "high-capacity",
      name: "High Capacity",
      description: "Teachers with 20+ available hours",
      filters: [
        {
          id: "availableHours",
          value: "20",
          variant: "number",
          operator: "gte",
          filterId: "preset-capacity"
        }
      ]
    }
  ],
  students: [
    {
      id: "active-students",
      name: "Active Students",
      description: "Show only active students",
      filters: [
        {
          id: "status",
          value: "ACTIVE",
          variant: "select",
          operator: "eq",
          filterId: "preset-active"
        }
      ],
      isDefault: true
    },
    {
      id: "new-students",
      name: "New Students",
      description: "Students enrolled this year",
      filters: [
        {
          id: "enrollmentYear",
          value: new Date().getFullYear().toString(),
          variant: "number",
          operator: "eq",
          filterId: "preset-new"
        }
      ]
    },
    {
      id: "senior-students",
      name: "Senior Students",
      description: "Grade 11 and 12 students",
      filters: [
        {
          id: "gradeLevel",
          value: ["11", "12"],
          variant: "multiSelect",
          operator: "in",
          filterId: "preset-senior"
        }
      ]
    }
  ],
  staff: [
    {
      id: "active-staff",
      name: "Active Staff",
      description: "Show only active staff members",
      filters: [
        {
          id: "status",
          value: "ACTIVE",
          variant: "select",
          operator: "eq",
          filterId: "preset-active"
        }
      ],
      isDefault: true
    },
    {
      id: "admin-staff",
      name: "Admin Staff",
      description: "Administrative staff members",
      filters: [
        {
          id: "staffType",
          value: "ADMINISTRATION",
          variant: "select",
          operator: "eq",
          filterId: "preset-admin"
        }
      ]
    }
  ]
};

interface FilterPresetsProps<TData> {
  table: Table<TData>;
  entityType: 'teachers' | 'students' | 'staff' | 'parents';
  className?: string;
}

export function FilterPresets<TData>({ 
  table, 
  entityType, 
  className 
}: FilterPresetsProps<TData>) {
  const columns = React.useMemo(() => {
    return table.getAllColumns().filter((column) => column.columnDef.enableColumnFilter);
  }, [table]);

  const [filters, setFilters] = useQueryState(
    "filters",
    getFiltersStateParser<TData>(columns.map((field) => field.id))
      .withDefault([])
      .withOptions({
        clearOnDefault: true,
        shallow: true,
      }),
  );

  const presets = COMMON_PRESETS[entityType] || [];

  const applyPreset = React.useCallback((preset: FilterPreset) => {
    console.log(`📋 FilterPresets - Applying preset: ${preset.name}`, preset.filters);
    setFilters(preset.filters);
  }, [setFilters]);

  const clearFilters = React.useCallback(() => {
    console.log("🗑️ FilterPresets - Clearing all filters");
    setFilters([]);
  }, [setFilters]);

  const saveAsPreset = React.useCallback(() => {
    // This would open a dialog to save current filters as a preset
    // For now, just show a placeholder
    console.log("💾 FilterPresets - Save current filters as preset");
    // Implementation would save to localStorage or backend
  }, []);

  const activeFiltersCount = filters.length;

  return (
    <div className={`flex items-center gap-2 ${className}`}>
      {/* Active Filters Count */}
      {activeFiltersCount > 0 && (
        <Badge variant="secondary" className="text-xs">
          {activeFiltersCount} filter{activeFiltersCount !== 1 ? 's' : ''}
        </Badge>
      )}

      {/* Presets Dropdown */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="outline" size="sm" className="h-8">
            <Filter className="mr-2 h-3 w-3" />
            Filter Presets
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start" className="w-56">
          <DropdownMenuLabel>Quick Filters</DropdownMenuLabel>
          <DropdownMenuSeparator />
          
          <DropdownMenuGroup>
            {presets.map((preset) => (
              <DropdownMenuItem
                key={preset.id}
                onClick={() => applyPreset(preset)}
                className="flex items-center justify-between"
              >
                <div className="flex flex-col">
                  <div className="flex items-center gap-2">
                    {preset.isDefault && <Star className="h-3 w-3 text-yellow-500" />}
                    <span className="font-medium">{preset.name}</span>
                  </div>
                  {preset.description && (
                    <span className="text-xs text-muted-foreground">
                      {preset.description}
                    </span>
                  )}
                </div>
              </DropdownMenuItem>
            ))}
          </DropdownMenuGroup>

          {presets.length > 0 && <DropdownMenuSeparator />}
          
          <DropdownMenuGroup>
            {activeFiltersCount > 0 && (
              <DropdownMenuItem onClick={saveAsPreset}>
                <Save className="mr-2 h-3 w-3" />
                Save Current Filters
              </DropdownMenuItem>
            )}
            
            {activeFiltersCount > 0 && (
              <DropdownMenuItem onClick={clearFilters}>
                <Trash2 className="mr-2 h-3 w-3" />
                Clear All Filters
              </DropdownMenuItem>
            )}
          </DropdownMenuGroup>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}
