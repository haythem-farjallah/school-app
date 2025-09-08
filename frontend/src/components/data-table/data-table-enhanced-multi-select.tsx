"use client";

import * as React from "react";
import type { Column } from "@tanstack/react-table";
import { Check, X, Search, Filter, ChevronDown, Plus } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import type { Option } from "@/types/data-table";

interface DataTableEnhancedMultiSelectProps<TData, TValue> {
  column?: Column<TData, TValue>;
  title?: string;
  options: Option[];
  placeholder?: string;
  searchPlaceholder?: string;
  maxDisplayItems?: number;
  allowCustomValues?: boolean;
}

export function DataTableEnhancedMultiSelect<TData, TValue>({
  column,
  title,
  options,
  placeholder = "Select options...",
  searchPlaceholder = "Search options...",
  maxDisplayItems = 3,
  allowCustomValues = false,
}: DataTableEnhancedMultiSelectProps<TData, TValue>) {
  const [open, setOpen] = React.useState(false);
  const [searchValue, setSearchValue] = React.useState("");
  const [customValue, setCustomValue] = React.useState("");

  const columnFilterValue = column?.getFilterValue();
  const selectedValues = new Set(
    Array.isArray(columnFilterValue) ? columnFilterValue : [],
  );

  // Filter options based on search
  const filteredOptions = React.useMemo(() => {
    if (!searchValue) return options;
    return options.filter((option) =>
      option.label.toLowerCase().includes(searchValue.toLowerCase()) ||
      option.value.toLowerCase().includes(searchValue.toLowerCase())
    );
  }, [options, searchValue]);

  const onItemSelect = React.useCallback(
    (optionValue: string, isSelected: boolean) => {
      if (!column) return;

      const newSelectedValues = new Set(selectedValues);
      if (isSelected) {
        newSelectedValues.delete(optionValue);
      } else {
        newSelectedValues.add(optionValue);
      }
      const filterValues = Array.from(newSelectedValues);
      column.setFilterValue(filterValues.length ? filterValues : undefined);
    },
    [column, selectedValues],
  );

  const onSelectAll = React.useCallback(() => {
    if (!column) return;
    const allValues = filteredOptions.map(option => option.value);
    column.setFilterValue(allValues);
  }, [column, filteredOptions]);

  const onClearAll = React.useCallback(() => {
    if (!column) return;
    column.setFilterValue(undefined);
  }, [column]);

  const onAddCustomValue = React.useCallback(() => {
    if (!customValue.trim() || !allowCustomValues || !column) return;
    
    const newSelectedValues = new Set(selectedValues);
    newSelectedValues.add(customValue.trim());
    const filterValues = Array.from(newSelectedValues);
    column.setFilterValue(filterValues);
    setCustomValue("");
  }, [customValue, allowCustomValues, column, selectedValues]);

  const selectedOptions = React.useMemo(() => {
    return options.filter(option => selectedValues.has(option.value));
  }, [options, selectedValues]);

  const displayText = React.useMemo(() => {
    if (selectedValues.size === 0) return placeholder;
    
    if (selectedValues.size <= maxDisplayItems) {
      return selectedOptions.map(option => option.label).join(", ");
    }
    
    return `${selectedOptions.slice(0, maxDisplayItems).map(option => option.label).join(", ")} +${selectedValues.size - maxDisplayItems} more`;
  }, [selectedValues.size, selectedOptions, maxDisplayItems, placeholder]);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          role="combobox"
          aria-expanded={open}
          className={cn(
            "justify-between min-w-[200px] max-w-[300px] h-auto py-2 px-3",
            "border-dashed hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 transition-all duration-200 shadow-sm hover:shadow-md",
            selectedValues.size > 0 && "bg-gradient-to-r from-blue-100/80 to-indigo-100/80 border-blue-300/60 shadow-md"
          )}
        >
          <div className="flex items-center gap-2 flex-1 min-w-0">
            <Filter className="h-4 w-4 text-blue-600 flex-shrink-0" />
            <span className="font-medium text-sm flex-shrink-0">{title}</span>
            {selectedValues.size > 0 && (
              <>
                <Separator orientation="vertical" className="h-4 bg-blue-300/60" />
                <span className="text-sm truncate" title={displayText}>
                  {displayText}
                </span>
              </>
            )}
          </div>
          <div className="flex items-center gap-1 flex-shrink-0">
            {selectedValues.size > 0 && (
              <Badge className="bg-blue-500 text-white text-xs">
                {selectedValues.size}
              </Badge>
            )}
            <ChevronDown className="h-4 w-4 opacity-50" />
          </div>
        </Button>
      </PopoverTrigger>
      <PopoverContent 
        className="w-[350px] p-0 bg-white/95 backdrop-blur-sm border border-slate-200/80 shadow-xl rounded-xl" 
        align="start"
      >
        <div className="p-4 space-y-4">
          {/* Header */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Filter className="h-4 w-4 text-slate-500" />
              <h3 className="font-semibold text-slate-800">{title}</h3>
            </div>
            {selectedValues.size > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={onClearAll}
                className="text-xs text-red-600 hover:text-red-700 hover:bg-red-50 transition-colors duration-200"
              >
                <X className="h-3 w-3 mr-1" />
                Clear
              </Button>
            )}
          </div>

          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
            <Input
              placeholder={searchPlaceholder}
              value={searchValue}
              onChange={(e) => setSearchValue(e.target.value)}
              className="pl-10 border-slate-300/80 focus:border-blue-400 focus:ring-2 focus:ring-blue-200/60 transition-all duration-200"
            />
          </div>

          {/* Bulk Actions */}
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={onSelectAll}
              disabled={filteredOptions.length === 0}
              className="text-xs hover:bg-blue-50 hover:text-blue-700 transition-colors duration-200"
            >
              <Check className="h-3 w-3 mr-1" />
              Select All ({filteredOptions.length})
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={onClearAll}
              disabled={selectedValues.size === 0}
              className="text-xs hover:bg-red-50 hover:text-red-700 transition-colors duration-200"
            >
              <X className="h-3 w-3 mr-1" />
              Clear All
            </Button>
          </div>

          {/* Options List */}
          <ScrollArea className="h-[250px] w-full">
            <div className="space-y-1 p-1">
              <AnimatePresence>
                {filteredOptions.map((option) => {
                  const isSelected = selectedValues.has(option.value);
                  
                  return (
                    <motion.div
                      key={option.value}
                      initial={{ opacity: 0, y: -10 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: -10 }}
                      transition={{ duration: 0.15 }}
                    >
                      <div
                        className={cn(
                          "flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-all duration-200",
                          "hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50",
                          isSelected && "bg-gradient-to-r from-blue-100/80 to-indigo-100/80 border border-blue-200/60"
                        )}
                        onClick={() => onItemSelect(option.value, isSelected)}
                      >
                        <Checkbox
                          checked={isSelected}
                          onChange={() => onItemSelect(option.value, isSelected)}
                          className="data-[state=checked]:bg-blue-500 data-[state=checked]:border-blue-500"
                        />
                        {option.icon && (
                          <option.icon className="h-4 w-4 text-slate-600" />
                        )}
                        <div className="flex-1 min-w-0">
                          <span className={cn(
                            "font-medium truncate block",
                            isSelected ? "text-blue-900" : "text-slate-700"
                          )}>
                            {option.label}
                          </span>
                          {option.description && (
                            <span className="text-xs text-slate-500 truncate block">
                              {option.description}
                            </span>
                          )}
                        </div>
                        {option.count && (
                          <Badge variant="outline" className="text-xs bg-slate-50 text-slate-600">
                            {option.count}
                          </Badge>
                        )}
                      </div>
                    </motion.div>
                  );
                })}
              </AnimatePresence>
              
              {filteredOptions.length === 0 && (
                <div className="text-center py-6 text-slate-500">
                  <Search className="h-8 w-8 mx-auto mb-2 text-slate-300" />
                  <p className="text-sm">No options found</p>
                  <p className="text-xs text-slate-400">Try adjusting your search</p>
                </div>
              )}
            </div>
          </ScrollArea>

          {/* Custom Value Input */}
          {allowCustomValues && (
            <>
              <Separator />
              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-700">Add Custom Value</label>
                <div className="flex gap-2">
                  <Input
                    placeholder="Enter custom value..."
                    value={customValue}
                    onChange={(e) => setCustomValue(e.target.value)}
                    className="flex-1 text-sm border-slate-300/80 focus:border-blue-400 focus:ring-2 focus:ring-blue-200/60"
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();
                        onAddCustomValue();
                      }
                    }}
                  />
                  <Button
                    size="sm"
                    onClick={onAddCustomValue}
                    disabled={!customValue.trim()}
                    className="bg-blue-500 hover:bg-blue-600 text-white"
                  >
                    <Plus className="h-3 w-3" />
                  </Button>
                </div>
              </div>
            </>
          )}

          {/* Selected Summary */}
          {selectedValues.size > 0 && (
            <>
              <Separator />
              <div className="text-xs text-slate-600 bg-slate-50/80 rounded-lg p-2">
                <span className="font-medium">{selectedValues.size} item{selectedValues.size !== 1 ? 's' : ''} selected</span>
                {selectedValues.size > maxDisplayItems && (
                  <span className="ml-2 text-slate-500">
                    (showing first {maxDisplayItems} in filter button)
                  </span>
                )}
              </div>
            </>
          )}
        </div>
      </PopoverContent>
    </Popover>
  );
}
