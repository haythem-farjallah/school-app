/**
 * Format a date to a string
 */
export function formatDate(
  date: Date | string | number,
  options: Intl.DateTimeFormatOptions = {}
): string {
  const defaultOptions: Intl.DateTimeFormatOptions = {
    year: "numeric",
    month: "short",
    day: "numeric",
  };

  const formatOptions = { ...defaultOptions, ...options };

  try {
    const dateObj = typeof date === "string" || typeof date === "number" ? new Date(date) : date;
    
    if (isNaN(dateObj.getTime())) {
      return "Invalid Date";
    }

    return new Intl.DateTimeFormat("en-US", formatOptions).format(dateObj);
  } catch (error) {
    console.error("Error formatting date:", error);
    return "Invalid Date";
  }
}

/**
 * Format a date to ISO string (YYYY-MM-DD)
 */
export function formatDateToISO(date: Date | string | number): string {
  try {
    const dateObj = typeof date === "string" || typeof date === "number" ? new Date(date) : date;
    
    if (isNaN(dateObj.getTime())) {
      return "";
    }

    return dateObj.toISOString().split("T")[0];
  } catch (error) {
    console.error("Error formatting date to ISO:", error);
    return "";
  }
}

/**
 * Format a date with time
 */
export function formatDateTime(
  date: Date | string | number,
  options: Intl.DateTimeFormatOptions = {}
): string {
  const defaultOptions: Intl.DateTimeFormatOptions = {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  };

  return formatDate(date, { ...defaultOptions, ...options });
}

/**
 * Format a relative time (e.g., "2 hours ago")
 */
export function formatRelativeTime(date: Date | string | number): string {
  try {
    const dateObj = typeof date === "string" || typeof date === "number" ? new Date(date) : date;
    
    if (isNaN(dateObj.getTime())) {
      return "Invalid Date";
    }

    const now = new Date();
    const diffInSeconds = Math.floor((now.getTime() - dateObj.getTime()) / 1000);

    if (diffInSeconds < 60) {
      return "just now";
    }

    const diffInMinutes = Math.floor(diffInSeconds / 60);
    if (diffInMinutes < 60) {
      return `${diffInMinutes} minute${diffInMinutes > 1 ? "s" : ""} ago`;
    }

    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) {
      return `${diffInHours} hour${diffInHours > 1 ? "s" : ""} ago`;
    }

    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays < 30) {
      return `${diffInDays} day${diffInDays > 1 ? "s" : ""} ago`;
    }

    return formatDate(dateObj);
  } catch (error) {
    console.error("Error formatting relative time:", error);
    return "Invalid Date";
  }
} 