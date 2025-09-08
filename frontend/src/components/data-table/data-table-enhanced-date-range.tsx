"use client";

import * as React from "react";
import type { Column } from "@tanstack/react-table";
import { Calendar as CalendarIcon, X, Clock, ChevronRight } from "lucide-react";
import type { DateRange } from "react-day-picker";
import { addDays, subDays, startOfMonth, endOfMonth, startOfYear, endOfYear, startOfWeek, endOfWeek } from "date-fns";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Separator } from "@/components/ui/separator";
import { ScrollArea } from "@/components/ui/scroll-area";
import { formatDate } from "@/lib/format";
import { cn } from "@/lib/utils";

type DateSelection = Date[] | DateRange;

function getIsDateRange(value: DateSelection): value is DateRange {
  return value && typeof value === "object" && !Array.isArray(value);
}

function parseAsDate(timestamp: number | string | undefined): Date | undefined {
  if (!timestamp) return undefined;
  const numericTimestamp =
    typeof timestamp === "string" ? Number(timestamp) : timestamp;
  const date = new Date(numericTimestamp);
  return !Number.isNaN(date.getTime()) ? date : undefined;
}

function parseColumnFilterValue(value: unknown) {
  if (value === null || value === undefined) {
    return [];
  }

  if (Array.isArray(value)) {
    return value.map((item) => {
      if (typeof item === "number" || typeof item === "string") {
        return item;
      }
      return undefined;
    });
  }

  if (typeof value === "string" || typeof value === "number") {
    return [value];
  }

  return [];
}

interface DatePreset {
  label: string;
  value: DateRange;
  description?: string;
}

interface DataTableEnhancedDateRangeProps<TData> {
  column: Column<TData, unknown>;
  title?: string;
  multiple?: boolean;
  showPresets?: boolean;
  customPresets?: DatePreset[];
}

export function DataTableEnhancedDateRange<TData>({
  column,
  title,
  multiple = false,
  showPresets = true,
  customPresets = [],
}: DataTableEnhancedDateRangeProps<TData>) {
  const [open, setOpen] = React.useState(false);
  const columnFilterValue = column.getFilterValue();

  // Default presets
  const defaultPresets: DatePreset[] = React.useMemo(() => {
    const today = new Date();
    return [
      {
        label: "Today",
        value: { from: today, to: today },
        description: "Just today"
      },
      {
        label: "Yesterday",
        value: { from: subDays(today, 1), to: subDays(today, 1) },
        description: "Yesterday only"
      },
      {
        label: "Last 7 days",
        value: { from: subDays(today, 6), to: today },
        description: "Past week including today"
      },
      {
        label: "Last 30 days",
        value: { from: subDays(today, 29), to: today },
        description: "Past month including today"
      },
      {
        label: "This week",
        value: { from: startOfWeek(today), to: endOfWeek(today) },
        description: "Current week"
      },
      {
        label: "This month",
        value: { from: startOfMonth(today), to: endOfMonth(today) },
        description: "Current month"
      },
      {
        label: "This year",
        value: { from: startOfYear(today), to: endOfYear(today) },
        description: "Current year"
      },
      {
        label: "Last month",
        value: { 
          from: startOfMonth(subDays(startOfMonth(today), 1)), 
          to: endOfMonth(subDays(startOfMonth(today), 1)) 
        },
        description: "Previous month"
      },
    ];
  }, []);

  const allPresets = [...defaultPresets, ...customPresets];

  const selectedDates = React.useMemo<DateSelection>(() => {
    if (!columnFilterValue) {
      return multiple ? { from: undefined, to: undefined } : [];
    }

    if (multiple) {
      const timestamps = parseColumnFilterValue(columnFilterValue);
      return {
        from: parseAsDate(timestamps[0]),
        to: parseAsDate(timestamps[1]),
      };
    }

    const timestamps = parseColumnFilterValue(columnFilterValue);
    const date = parseAsDate(timestamps[0]);
    return date ? [date] : [];
  }, [columnFilterValue, multiple]);

  const onSelect = React.useCallback(
    (date: Date | DateRange | undefined) => {
      if (!date) {
        column.setFilterValue(undefined);
        return;
      }

      if (multiple && !("getTime" in date)) {
        const from = date.from?.getTime();
        const to = date.to?.getTime();
        column.setFilterValue(from || to ? [from, to] : undefined);
      } else if (!multiple && "getTime" in date) {
        column.setFilterValue(date.getTime());
      }
    },
    [column, multiple],
  );

  const onPresetSelect = React.useCallback((preset: DatePreset) => {
    const from = preset.value.from?.getTime();
    const to = preset.value.to?.getTime();
    column.setFilterValue([from, to]);
    setOpen(false);
  }, [column]);

  const onReset = React.useCallback(
    (event?: React.MouseEvent) => {
      event?.stopPropagation();
      column.setFilterValue(undefined);
    },
    [column],
  );

  const hasValue = React.useMemo(() => {
    if (multiple) {
      if (!getIsDateRange(selectedDates)) return false;
      return selectedDates.from || selectedDates.to;
    }
    if (!Array.isArray(selectedDates)) return false;
    return selectedDates.length > 0;
  }, [multiple, selectedDates]);

  const formatDateRange = React.useCallback((range: DateRange) => {
    if (!range.from && !range.to) return "";
    if (range.from && range.to) {
      if (range.from.getTime() === range.to.getTime()) {
        return formatDate(range.from);
      }
      return `${formatDate(range.from)} - ${formatDate(range.to)}`;
    }
    return formatDate(range.from ?? range.to);
  }, []);

  const displayLabel = React.useMemo(() => {
    if (multiple) {
      if (!getIsDateRange(selectedDates)) return title;
      const hasSelectedDates = selectedDates.from || selectedDates.to;
      return hasSelectedDates ? formatDateRange(selectedDates) : title;
    }

    if (getIsDateRange(selectedDates)) return title;
    const hasSelectedDate = selectedDates.length > 0;
    return hasSelectedDate ? formatDate(selectedDates[0]) : title;
  }, [selectedDates, multiple, formatDateRange, title]);

  // Check if current selection matches a preset
  const matchingPreset = React.useMemo(() => {
    if (!hasValue || !getIsDateRange(selectedDates)) return null;
    
    return allPresets.find(preset => {
      const presetFrom = preset.value.from?.getTime();
      const presetTo = preset.value.to?.getTime();
      const selectedFrom = selectedDates.from?.getTime();
      const selectedTo = selectedDates.to?.getTime();
      
      return presetFrom === selectedFrom && presetTo === selectedTo;
    });
  }, [hasValue, selectedDates, allPresets]);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button 
          variant="outline" 
          size="sm" 
          className={cn(
            "border-dashed hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 transition-all duration-200 shadow-sm hover:shadow-md",
            hasValue && "bg-gradient-to-r from-blue-100/80 to-indigo-100/80 border-blue-300/60 shadow-md"
          )}
        >
          <CalendarIcon className="h-4 w-4 text-blue-600 mr-2" />
          <span className="font-medium">{title}</span>
          {hasValue && (
            <>
              <Separator
                orientation="vertical"
                className="mx-2 data-[orientation=vertical]:h-4 bg-blue-300/60"
              />
              <div className="flex items-center gap-2">
                {matchingPreset && (
                  <Badge className="bg-green-50 text-green-700 border border-green-200/60 text-xs">
                    {matchingPreset.label}
                  </Badge>
                )}
                <Badge className="bg-blue-50 text-blue-700 border border-blue-200/60 font-mono text-xs">
                  {displayLabel}
                </Badge>
              </div>
            </>
          )}
          {hasValue && (
            <Button
              variant="ghost"
              size="sm"
              onClick={onReset}
              className="ml-2 h-5 w-5 p-0 hover:bg-red-100/80 hover:text-red-600 transition-all duration-200 hover:scale-110"
            >
              <X className="h-3 w-3" />
            </Button>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent 
        className="w-auto p-0 bg-white/95 backdrop-blur-sm border border-slate-200/80 shadow-xl rounded-xl" 
        align="start"
      >
        <div className="flex">
          {/* Presets Sidebar */}
          {showPresets && (
            <div className="w-64 border-r border-slate-200/60 bg-gradient-to-b from-slate-50/80 to-blue-50/40">
              <div className="p-4 border-b border-slate-200/60">
                <div className="flex items-center gap-2">
                  <Clock className="h-4 w-4 text-slate-500" />
                  <h3 className="font-semibold text-slate-800">Quick Presets</h3>
                </div>
                <p className="text-xs text-slate-600 mt-1">Choose a common date range</p>
              </div>
              <ScrollArea className="h-[300px]">
                <div className="p-2 space-y-1">
                  {allPresets.map((preset, index) => {
                    const isSelected = matchingPreset?.label === preset.label;
                    
                    return (
                      <Button
                        key={index}
                        variant="ghost"
                        size="sm"
                        onClick={() => onPresetSelect(preset)}
                        className={cn(
                          "w-full justify-start text-left h-auto p-3 transition-all duration-200",
                          "hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50",
                          isSelected && "bg-gradient-to-r from-blue-100/80 to-indigo-100/80 border border-blue-200/60"
                        )}
                      >
                        <div className="flex items-center justify-between w-full">
                          <div className="flex-1 min-w-0">
                            <div className={cn(
                              "font-medium text-sm truncate",
                              isSelected ? "text-blue-900" : "text-slate-700"
                            )}>
                              {preset.label}
                            </div>
                            {preset.description && (
                              <div className="text-xs text-slate-500 truncate mt-0.5">
                                {preset.description}
                              </div>
                            )}
                          </div>
                          <ChevronRight className={cn(
                            "h-3 w-3 flex-shrink-0 ml-2",
                            isSelected ? "text-blue-600" : "text-slate-400"
                          )} />
                        </div>
                      </Button>
                    );
                  })}
                </div>
              </ScrollArea>
            </div>
          )}

          {/* Calendar */}
          <div className="p-4">
            <div className="text-center mb-3">
              <h3 className="font-semibold text-lg text-slate-800">{title}</h3>
              <p className="text-sm text-slate-600">
                {multiple ? "Select a date range" : "Select a date"}
              </p>
            </div>
            
            {multiple ? (
              <Calendar
                initialFocus
                mode="range"
                selected={
                  getIsDateRange(selectedDates)
                    ? selectedDates
                    : { from: undefined, to: undefined }
                }
                onSelect={onSelect}
                numberOfMonths={showPresets ? 1 : 2}
                className="rounded-lg"
              />
            ) : (
              <Calendar
                initialFocus
                mode="single"
                selected={
                  !getIsDateRange(selectedDates) ? selectedDates[0] : undefined
                }
                onSelect={onSelect}
                className="rounded-lg"
              />
            )}

            {hasValue && (
              <div className="mt-4 pt-4 border-t border-slate-200/60">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={onReset}
                  className="w-full bg-gradient-to-r from-red-50 to-pink-50 hover:from-red-100 hover:to-pink-100 border-red-200/60 text-red-700 hover:text-red-800 transition-all duration-200"
                >
                  <X className="h-4 w-4 mr-2" />
                  Clear Date Filter
                </Button>
              </div>
            )}
          </div>
        </div>
      </PopoverContent>
    </Popover>
  );
}
