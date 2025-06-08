import type { Column } from "@tanstack/react-table";

/**
 * Returns the common pinning styles for a column
 */
export function getCommonPinningStyles<TData>({
  column,
}: {
  column: Column<TData>;
}) {
  const isPinned = column.getIsPinned();
  const isLastLeftPinnedColumn = isPinned === "left" && column.getIsLastColumn("left");
  const isFirstRightPinnedColumn = isPinned === "right" && column.getIsFirstColumn("right");

  return {
    boxShadow: isLastLeftPinnedColumn
      ? "-4px 0 4px -4px hsl(var(--border)) inset"
      : isFirstRightPinnedColumn
      ? "4px 0 4px -4px hsl(var(--border)) inset"
      : undefined,
    left: isPinned === "left" ? `${column.getStart("left")}px` : undefined,
    right: isPinned === "right" ? `${column.getAfter("right")}px` : undefined,
    opacity: isPinned ? 0.97 : 1,
    position: isPinned ? ("sticky" as const) : ("relative" as const),
    background: isPinned ? "hsl(var(--background))" : undefined,
    width: column.getSize(),
    zIndex: isPinned ? 1 : 0,
  };
}

/**
 * Filter operators for different data types
 */
export const filterOperators = {
  text: [
    { label: "Contains", value: "contains" },
    { label: "Does not contain", value: "notContains" },
    { label: "Is", value: "is" },
    { label: "Is not", value: "isNot" },
    { label: "Starts with", value: "startsWith" },
    { label: "Ends with", value: "endsWith" },
    { label: "Is empty", value: "isEmpty" },
    { label: "Is not empty", value: "isNotEmpty" },
  ],
  number: [
    { label: "=", value: "equals" },
    { label: "≠", value: "notEquals" },
    { label: ">", value: "greaterThan" },
    { label: "≥", value: "greaterThanOrEqual" },
    { label: "<", value: "lessThan" },
    { label: "≤", value: "lessThanOrEqual" },
    { label: "Is empty", value: "isEmpty" },
    { label: "Is not empty", value: "isNotEmpty" },
  ],
  date: [
    { label: "Is", value: "is" },
    { label: "Is not", value: "isNot" },
    { label: "Is after", value: "isAfter" },
    { label: "Is on or after", value: "isOnOrAfter" },
    { label: "Is before", value: "isBefore" },
    { label: "Is on or before", value: "isOnOrBefore" },
    { label: "Is between", value: "isBetween" },
    { label: "Is empty", value: "isEmpty" },
    { label: "Is not empty", value: "isNotEmpty" },
  ],
  select: [
    { label: "Is", value: "is" },
    { label: "Is not", value: "isNot" },
    { label: "Is any of", value: "isAnyOf" },
    { label: "Is none of", value: "isNoneOf" },
    { label: "Is empty", value: "isEmpty" },
    { label: "Is not empty", value: "isNotEmpty" },
  ],
  multiSelect: [
    { label: "Contains", value: "contains" },
    { label: "Does not contain", value: "notContains" },
    { label: "Contains any of", value: "containsAnyOf" },
    { label: "Contains all of", value: "containsAllOf" },
    { label: "Is empty", value: "isEmpty" },
    { label: "Is not empty", value: "isNotEmpty" },
  ],
} as const;

/**
 * Returns the filter operators for a given data type
 */
export function getFilterOperators(dataType: keyof typeof filterOperators) {
  return filterOperators[dataType] || filterOperators.text;
}

/**
 * Returns the default filter operator for a given data type
 */
export function getDefaultFilterOperator(dataType: keyof typeof filterOperators) {
  const operators = getFilterOperators(dataType);
  return operators[0]?.value || "contains";
}

/**
 * Type definitions for filter operators
 */
export type FilterOperator = {
  label: string;
  value: string;
};

export type DataType = keyof typeof filterOperators; 