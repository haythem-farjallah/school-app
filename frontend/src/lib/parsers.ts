import { 
  parseAsArrayOf, 
  parseAsString, 
  type Parser,
  createParser,
} from "nuqs";
import type { ExtendedColumnSort } from "@/types/data-table";

/**
 * Parser for sorting state
 */
export function getSortingStateParser(
  columnIds: Set<string>
): Parser<ExtendedColumnSort[]> {
  return createParser({
    parse: (value: string): ExtendedColumnSort[] => {
      if (!value) return [];
      
      return value
        .split(",")
        .map((sortStr) => {
          const [id, desc] = sortStr.split(":");
          if (columnIds.has(id)) {
            return {
              id,
              desc: desc === "true",
            } as ExtendedColumnSort;
          }
          return null;
        })
        .filter(Boolean) as ExtendedColumnSort[];
    },
    serialize: (value: ExtendedColumnSort[]): string => {
      return value
        .map((sort) => `${sort.id}:${sort.desc}`)
        .join(",");
    },
  });
}

/**
 * Parser for filters state
 */
export function getFiltersStateParser(enableAdvancedFilter = false) {
  if (enableAdvancedFilter) {
    return parseAsString.withDefault("");
  }
  
  return parseAsArrayOf(parseAsString, ",").withDefault([]);
}

/**
 * Parser for column visibility state
 */
export function getColumnVisibilityParser() {
  return createParser({
    parse: (value: string): Record<string, boolean> => {
      if (!value) return {};
      
      const hiddenColumns = value.split(",").filter(Boolean);
      const visibility: Record<string, boolean> = {};
      
      hiddenColumns.forEach((columnId: string) => {
        visibility[columnId] = false;
      });
      
      return visibility;
    },
    serialize: (value: Record<string, boolean>): string => {
      return Object.entries(value)
        .filter(([, visible]) => !visible)
        .map(([id]) => id)
        .join(",");
    },
  }).withDefault({});
}

/**
 * Parser for row selection state
 */
export function getRowSelectionParser() {
  return {
    serialize: (value: Record<string, boolean>): string => {
      return Object.keys(value)
        .filter((key) => value[key])
        .join(",");
    },
    parse: (value: string): Record<string, boolean> => {
      if (!value) return {};
      
      const selectedRows = value.split(",").filter(Boolean);
      const selection: Record<string, boolean> = {};
      
      selectedRows.forEach((rowId: string) => {
        selection[rowId] = true;
      });
      
      return selection;
    },
  };
}

/**
 * Parser for data table state combining all parsers
 */
export function getDataTableStateParser(
  columnIds: Set<string>,
  enableAdvancedFilter = false
) {
  return {
    sorting: getSortingStateParser(columnIds),
    filters: getFiltersStateParser(enableAdvancedFilter),
    columnVisibility: getColumnVisibilityParser(),
    rowSelection: getRowSelectionParser(),
  };
} 