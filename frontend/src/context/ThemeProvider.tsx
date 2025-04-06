import React, { useEffect, useState } from "react";
import { ThemeContext, Theme } from "./ThemeContext";

export const ThemeProvider: React.FC<React.PropsWithChildren> = ({
  children,
}) => {
  // localStorage  ➜  prefers‑color‑scheme  ➜  default "light"
  const getInitial = (): Theme =>
    (localStorage.getItem("theme") as Theme | null) ??
    (window.matchMedia("(prefers-color-scheme: dark)").matches
      ? "dark"
      : "light");

  const [theme, setTheme] = useState<Theme>(getInitial);

  // Apply/remove the "dark" class and persist choice
  useEffect(() => {
    document.documentElement.classList.toggle("dark", theme === "dark");
    localStorage.setItem("theme", theme);
  }, [theme]);

  const toggle = () => setTheme((t) => (t === "dark" ? "light" : "dark"));

  return (
    <ThemeContext.Provider value={{ theme, toggle }}>
      {children}
    </ThemeContext.Provider>
  );
};
