import { AxiosError } from "axios";

// For automatic/background requests - very conservative about showing errors
export const getErrorMessage = (e: AxiosError): string => {
  // Check for specific error responses
  const responseData = e.response?.data as { message?: string; error?: string } | undefined;
  
  // Silent errors - don't show notifications for these
  if (e.response?.status === 404) {
    console.warn('Resource not found:', e.config?.url);
    return ""; // Return empty string to prevent notification
  }
  
  // Server errors (500+) - log but don't notify user for background requests
  if (e.response?.status >= 500) {
    console.error('Server error:', e.response?.status, e.config?.url, responseData);
    return ""; // Return empty string to prevent notification
  }
  
  // Network errors - don't notify for background requests
  if (!e.response) {
    console.error('Network error:', e.config?.url);
    return ""; // Return empty string to prevent notification
  }
  
  // Only show notifications for client errors that users can fix (400-499, excluding 404)
  if (e.response?.status >= 400 && e.response?.status < 500 && e.response?.status !== 404) {
    return responseData?.message ?? 
           responseData?.error ?? 
           "Please check your input and try again.";
  }
  
  // For all other cases, log but don't notify
  console.warn('HTTP error:', e.response?.status, e.config?.url, responseData);
  return "";
};

// For user-initiated actions - more permissive about showing errors
export const getUserActionErrorMessage = (e: AxiosError): string => {
  const responseData = e.response?.data as { message?: string; error?: string } | undefined;
  
  // Still silent on 404s
  if (e.response?.status === 404) {
    console.warn('Resource not found:', e.config?.url);
    return "The requested resource was not found.";
  }
  
  // Network errors - show for user actions
  if (!e.response) {
    return "Unable to connect to server. Please check your internet connection.";
  }
  
  // Server errors - show generic message for user actions
  if (e.response?.status >= 500) {
    return "Server error occurred. Please try again later.";
  }
  
  // Client errors - show detailed message
  if (e.response?.status >= 400 && e.response?.status < 500) {
    return responseData?.message ?? 
           responseData?.error ?? 
           "Please check your input and try again.";
  }
  
  // Default fallback
  return "An error occurred. Please try again.";
};