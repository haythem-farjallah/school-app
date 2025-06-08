import { createContext, useContext } from "react";

/* -------------------------------------------------------------------------- */
/*  Public types                                                              */
/* -------------------------------------------------------------------------- */
export type Theme = "light" | "dark";

export interface ThemeCtx {
  /** current mode */
  theme: Theme;
  /** swap between light ↔︎ dark */
  toggle: () => void;
}

/* -------------------------------------------------------------------------- */
/*  Context + hook                                                            */
/* -------------------------------------------------------------------------- */
export const ThemeContext = createContext<ThemeCtx | undefined>(undefined);

export const useTheme = (): ThemeCtx => {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme must be used inside <ThemeProvider>");
  return ctx;
};
