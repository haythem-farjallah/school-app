import { store } from "../stores/store";
import { addNotification } from "../stores/notificationSlice";

export const notifyError = (msg: string) =>
  store.dispatch(addNotification({ type: "error", title: "Error", message: msg }));

export const notifySuccess = (msg: string) =>
  store.dispatch(addNotification({ type: "success", title: "Success", message: msg }));

export const notifyInfo = (msg: string) =>
  store.dispatch(addNotification({ type: "info", title: "Info", message: msg }));

export const notifyWarning = (msg: string) =>
  store.dispatch(addNotification({ type: "warning", title: "Warning", message: msg }));