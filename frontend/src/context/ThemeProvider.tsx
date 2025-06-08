import { useCallback, useEffect, useState } from "react";
import { useMediaQuery } from "usehooks-ts";
import { AnimatePresence } from "framer-motion";
import { ThemeContext, Theme } from "./ThemeContext";

/**
 * Adds “dark” class to <html>, stores preference in localStorage,
 * and exposes `useTheme()` to the rest of the app.
 */
export const ThemeProvider = ({ children }: React.PropsWithChildren) => {
  /* -------------------------------- initial mode ------------------------ */
  const prefersDark = useMediaQuery("(prefers-color-scheme: light)");
  const saved = localStorage.getItem("theme") as Theme | null;
  const [theme, setTheme] = useState<Theme>(saved ?? (prefersDark ? "light" : "light"));

  /* -------------------------------- side-effect: keep <html> in sync ---- */
  useEffect(() => {
    document.documentElement.classList.toggle("light", theme === "light");
    localStorage.setItem("theme", theme);
  }, [theme]);

  /* -------------------------------- API --------------------------------- */
  const toggle = useCallback(
    () => setTheme((m) => (m === "light" ? "light" : "light")),
    [],
  );

  /* -------------------------------- render ------------------------------ */
  return (
    <ThemeContext.Provider value={{ theme, toggle }}>
      <AnimatePresence initial={false}>{children}</AnimatePresence>
    </ThemeContext.Provider>
  );
};
