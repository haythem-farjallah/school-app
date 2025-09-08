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
  /** Whether the notification has been read */
  read?: boolean;
  /** When the notification was received */
  timestamp?: string;
  /** Action URL for navigation */
  actionUrl?: string;
  /** Source of the notification (websocket, api, etc.) */
  source?: 'websocket' | 'api' | 'system';
}

/* -------------------------------------------------------------------------- */
/*  Initial state                                                             */
/* -------------------------------------------------------------------------- */
interface NotificationState {
  list: Notification[];
  unreadCount: number;
}

const initialState: NotificationState = { list: [], unreadCount: 0 };

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
        payload: { 
          ...n, 
          id: nanoid(),
          read: false,
          timestamp: new Date().toISOString(),
          source: n.source || 'system'
        },
      }),
      reducer: (state, action: PayloadAction<Notification>) => {
        state.list.unshift(action.payload); // Add to beginning for recent-first order
        state.unreadCount += 1;
        
        // Keep only last 100 notifications
        if (state.list.length > 100) {
          state.list = state.list.slice(0, 100);
        }
      },
    },

    /** Remove a single notification by id */
    removeNotification: (state, action: PayloadAction<string>) => {
      const notification = state.list.find(n => n.id === action.payload);
      if (notification && !notification.read) {
        state.unreadCount = Math.max(0, state.unreadCount - 1);
      }
      state.list = state.list.filter((n) => n.id !== action.payload);
    },

    /** Mark a notification as read */
    markAsRead: (state, action: PayloadAction<string>) => {
      const notification = state.list.find(n => n.id === action.payload);
      if (notification && !notification.read) {
        notification.read = true;
        state.unreadCount = Math.max(0, state.unreadCount - 1);
      }
    },

    /** Mark all notifications as read */
    markAllAsRead: (state) => {
      state.list.forEach(n => n.read = true);
      state.unreadCount = 0;
    },

    /** Clear all notifications (e.g. on logout) */
    clearNotifications: (state) => {
      state.list = [];
      state.unreadCount = 0;
    },
  },
});

export const {
  addNotification,
  removeNotification,
  markAsRead,
  markAllAsRead,
  clearNotifications,
} = notificationSlice.actions;

export default notificationSlice.reducer;
