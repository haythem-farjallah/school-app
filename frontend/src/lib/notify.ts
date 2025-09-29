import { store } from "../stores/store";
import { addNotification } from "../stores/notificationSlice";

export const notifyError = (msg: string) => {
  // Don't show notifications for empty messages or generic server errors
  if (!msg || msg.trim() === "" || msg === "UNEXPECTED_ERROR") {
    return;
  }
  
  // Don't show notifications for generic server error messages that aren't user-actionable
  const genericMessages = [
    "Server error occurred. Please try again later.",
    "An error occurred. Please try again.",
    "Unknown error"
  ];
  
  if (genericMessages.includes(msg)) {
    console.warn("Suppressing generic error notification:", msg);
    return;
  }
  
  store.dispatch(addNotification({ type: "error", title: "Error", message: msg }));
};

export const notifySuccess = (msg: string) =>
  store.dispatch(addNotification({ type: "success", title: "Success", message: msg }));

export const notifyInfo = (msg: string) =>
  store.dispatch(addNotification({ type: "info", title: "Info", message: msg }));

// For user-initiated actions where we want to show errors
export const notifyUserActionError = (msg: string) => {
  if (!msg || msg.trim() === "") {
    return;
  }
  store.dispatch(addNotification({ type: "error", title: "Error", message: msg }));
};

export const notifyWarning = (msg: string) =>
  store.dispatch(addNotification({ type: "warning", title: "Warning", message: msg }));