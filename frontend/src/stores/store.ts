import { configureStore, Middleware } from "@reduxjs/toolkit";
import { TypedUseSelectorHook, useDispatch, useSelector } from "react-redux";
import authReducer, { accessTokenRefreshed, resetAuth } from "@/stores/authSlice";
import { setAccessTokenUpdatedHandler, setSessionEndedHandler } from "@/lib/session";

/* -------------------------------------------------------------------------- */
/*  Slice reducers                                                            */
/* -------------------------------------------------------------------------- */
import notificationReducer from "./notificationSlice";

// add more slices here as you create them

/* -------------------------------------------------------------------------- */
/*  Dev‑only logger middleware                                                */
/* -------------------------------------------------------------------------- */
const logger: Middleware =
  () => (next) => (action) => {
    if (import.meta.env.DEV) {
      console.log("%cRedux", "color: #4e9af1", action);
    }
    return next(action);
  };

/* -------------------------------------------------------------------------- */
/*  Store                                                                     */
/* -------------------------------------------------------------------------- */
export const store = configureStore({
  reducer: {
    notification: notificationReducer,
    auth: authReducer,
  },
  middleware: (getDefault) =>
    getDefault({ serializableCheck: false }).concat(logger),
  devTools: import.meta.env.DEV,
});

// Ending the session (logout or a rejected access token) also clears the in-memory auth state.
setSessionEndedHandler(() => store.dispatch(resetAuth()));
// A refreshed access token replaces the one in the in-memory auth state.
setAccessTokenUpdatedHandler((accessToken) => store.dispatch(accessTokenRefreshed(accessToken)));

/* -------------------------------------------------------------------------- */
/*  Typed hooks & helper types                                                */
/* -------------------------------------------------------------------------- */
export type RootState   = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

export const useAppDispatch: () => AppDispatch = useDispatch;
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
