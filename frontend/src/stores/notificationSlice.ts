import { createSlice, nanoid, PayloadAction } from "@reduxjs/toolkit";

/* -------------------------------------------------------------------------- */
/*  Types                                                                     */
/* -------------------------------------------------------------------------- */
export type NotificationType = "success" | "error" | "info" | "warning";

export interface Notification {
  /** Unique id so you can dismiss a single toast */
  id: string;
  type: NotificationType;
  title: string;
  message?: string;
  /** Auto‑dismiss timeout in ms (0 = stay until manually closed) */
  ttl?: number;
}

/* -------------------------------------------------------------------------- */
/*  Initial state                                                             */
/* -------------------------------------------------------------------------- */
interface NotificationState {
  list: Notification[];
}

const initialState: NotificationState = { list: [] };

/* -------------------------------------------------------------------------- */
/*  Slice                                                                     */
/* -------------------------------------------------------------------------- */
const notificationSlice = createSlice({
  name: "notification",
  initialState,
  reducers: {
    /** Push a new notification onto the stack */
    addNotification: {
      prepare: (n: Omit<Notification, "id">) => ({
        payload: { ...n, id: nanoid() },
      }),
      reducer: (state, action: PayloadAction<Notification>) => {
        state.list.push(action.payload);
      },
    },

    /** Remove a single notification by id */
    removeNotification: (state, action: PayloadAction<string>) => {
      state.list = state.list.filter((n) => n.id !== action.payload);
    },

    /** Clear all notifications (e.g. on logout) */
    clearNotifications: (state) => {
      state.list = [];
    },
  },
});

export const {
  addNotification,
  removeNotification,
  clearNotifications,
} = notificationSlice.actions;

export default notificationSlice.reducer;
