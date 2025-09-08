"use client";

import type { Option } from "@/types/data-table";
import type { Column } from "@tanstack/react-table";
import { Check, PlusCircle, XCircle, Filter, Search } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
  CommandSeparator,
} from "@/components/ui/command";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import * as React from "react";

interface DataTableFacetedFilterProps<TData, TValue> {
  column?: Column<TData, TValue>;
  title?: string;
  options: Option[];
  multiple?: boolean;
}

export function DataTableFacetedFilter<TData, TValue>({
  column,
  title,
  options,
  multiple,
}: DataTableFacetedFilterProps<TData, TValue>) {
  const [open, setOpen] = React.useState(false);

  const columnFilterValue = column?.getFilterValue();
  const selectedValues = new Set(
    Array.isArray(columnFilterValue) ? columnFilterValue : [],
  );

  const onItemSelect = React.useCallback(
    (option: Option, isSelected: boolean) => {
      if (!column) return;

      if (multiple) {
        const newSelectedValues = new Set(selectedValues);
        if (isSelected) {
          newSelectedValues.delete(option.value);
        } else {
          newSelectedValues.add(option.value);
        }
        const filterValues = Array.from(newSelectedValues);
        column.setFilterValue(filterValues.length ? filterValues : undefined);
      } else {
        column.setFilterValue(isSelected ? undefined : [option.value]);
        setOpen(false);
      }
    },
    [column, multiple, selectedValues],
  );

  const onReset = React.useCallback(
    (event?: React.MouseEvent) => {
      event?.stopPropagation();
      column?.setFilterValue(undefined);
    },
    [column],
  );

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button 
          variant="outline" 
          size="sm" 
          className={cn(
            "border-dashed hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 transition-all duration-200 shadow-sm hover:shadow-md",
            selectedValues?.size > 0 && "bg-gradient-to-r from-blue-100/80 to-indigo-100/80 border-blue-300/60 shadow-md"
          )}
        >
          {selectedValues?.size > 0 ? (
            <div
              role="button"
              aria-label={`Clear ${title} filter`}
              tabIndex={0}
              onClick={onReset}
              className="rounded-sm p-1 hover:bg-red-100/80 transition-all duration-200 hover:scale-110"
            >
              <XCircle className="h-4 w-4 text-red-600" />
            </div>
          ) : (
            <Filter className="h-4 w-4 text-blue-600" />
          )}
          <span className="font-medium">{title}</span>
          {selectedValues?.size > 0 && (
            <>
              <Separator
                orientation="vertical"
                className="mx-2 data-[orientation=vertical]:h-4 bg-blue-300/60"
              />
              <Badge
                variant="secondary"
                className="rounded-full px-2 py-1 font-semibold bg-blue-50 text-blue-700 border border-blue-200/60 lg:hidden"
              >
                {selectedValues.size}
              </Badge>
              <div className="hidden items-center gap-1 lg:flex">
                {selectedValues.size > 2 ? (
                  <Badge
                    variant="secondary"
                    className="rounded-full px-2 py-1 font-semibold bg-blue-50 text-blue-700 border border-blue-200/60"
                  >
                    {selectedValues.size} selected
                  </Badge>
                ) : (
                  options
                    .filter((option) => selectedValues.has(option.value))
                    .map((option) => (
                      <Badge
                        variant="secondary"
                        key={option.value}
                        className="rounded-full px-2 py-1 font-medium bg-gradient-to-r from-green-50 to-emerald-50 text-green-700 border border-green-200/60"
                      >
                        {option.label}
                      </Badge>
                    ))
                )}
              </div>
            </>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent 
        className="w-[280px] p-0 bg-white/95 backdrop-blur-sm border border-slate-200/80 shadow-xl rounded-xl" 
        align="start"
      >
        <Command className="rounded-xl">
          <div className="flex items-center border-b border-slate-200/60 px-3 py-2 bg-gradient-to-r from-slate-50/80 to-blue-50/40">
            <Search className="h-4 w-4 text-slate-500 mr-2" />
            <CommandInput 
              placeholder={`Search ${title?.toLowerCase()}...`} 
              className="border-0 bg-transparent focus:ring-0 placeholder:text-slate-400"
            />
          </div>
          <CommandList className="max-h-full">
            <CommandEmpty className="py-6 text-center text-slate-500">
              <div className="flex flex-col items-center gap-2">
                <Search className="h-8 w-8 text-slate-300" />
                <p className="text-sm">No options found</p>
                <p className="text-xs text-slate-400">Try adjusting your search</p>
              </div>
            </CommandEmpty>
            <CommandGroup className="max-h-[300px] overflow-y-auto overflow-x-hidden p-2">
              {options.map((option) => {
                const isSelected = selectedValues.has(option.value);

                return (
                  <CommandItem
                    key={option.value}
                    onSelect={() => onItemSelect(option, isSelected)}
                    className={cn(
                      "flex items-center gap-3 px-3 py-2.5 rounded-lg cursor-pointer transition-all duration-200",
                      "hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50",
                      isSelected && "bg-gradient-to-r from-blue-100/80 to-indigo-100/80 border border-blue-200/60"
                    )}
                  >
                    <div
                      className={cn(
                        "flex size-5 items-center justify-center rounded-md border-2 transition-all duration-200",
                        isSelected
                          ? "bg-gradient-to-r from-blue-500 to-indigo-500 border-blue-500 text-white shadow-sm"
                          : "border-slate-300 hover:border-blue-400 bg-white"
                      )}
                    >
                      <Check className={cn("h-3 w-3", isSelected ? "opacity-100" : "opacity-0")} />
                    </div>
                    {option.icon && (
                      <option.icon className="h-4 w-4 text-slate-600" />
                    )}
                    <span className={cn(
                      "truncate flex-1 font-medium",
                      isSelected ? "text-blue-900" : "text-slate-700"
                    )}>
                      {option.label}
                    </span>
                    {option.count && (
                      <Badge 
                        variant="outline" 
                        className="ml-auto font-mono text-xs bg-slate-50 text-slate-600 border-slate-200"
                      >
                        {option.count}
                      </Badge>
                    )}
                  </CommandItem>
                );
              })}
            </CommandGroup>
            {selectedValues.size > 0 && (
              <>
                <CommandSeparator className="bg-slate-200/60" />
                <CommandGroup className="p-2">
                  <CommandItem
                    onSelect={() => onReset()}
                    className="justify-center text-center py-2.5 rounded-lg hover:bg-gradient-to-r hover:from-red-50 hover:to-pink-50 text-red-600 hover:text-red-700 font-medium cursor-pointer transition-all duration-200"
                  >
                    <XCircle className="h-4 w-4 mr-2" />
                    Clear all filters
                  </CommandItem>
                </CommandGroup>
              </>
            )}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
