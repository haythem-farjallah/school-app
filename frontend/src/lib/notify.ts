import { store } from "../stores/store";
import { addNotification } from "../stores/notificationSlice";

export const notifyError = (msg: string) =>
  store.dispatch(addNotification({ type: "error", title: "Error", message: msg }));
