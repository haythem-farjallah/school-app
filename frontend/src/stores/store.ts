import { configureStore, Middleware } from "@reduxjs/toolkit";
import { TypedUseSelectorHook, useDispatch, useSelector } from "react-redux";

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
  },
  middleware: (getDefault) =>
    getDefault({ serializableCheck: false }).concat(logger),
  devTools: import.meta.env.DEV,
});

/* -------------------------------------------------------------------------- */
/*  Typed hooks & helper types                                                */
/* -------------------------------------------------------------------------- */
export type RootState   = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

export const useAppDispatch: () => AppDispatch = useDispatch;
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
