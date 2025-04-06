import { createContext } from "react";
import { supportedLocales, SupportedLocale } from "../services/i18n";

/* -------------------------------------------------------------------------- */
/*  Public types                                                              */
/* -------------------------------------------------------------------------- */

export interface LanguageContextValue {
  lang: SupportedLocale;
  setLang: (lng: SupportedLocale) => void;
  /** list of languages you support */
  available: typeof supportedLocales;
}

/* -------------------------------------------------------------------------- */
/*  The context                                                               */
/* -------------------------------------------------------------------------- */

export const LanguageContext = createContext<LanguageContextValue | undefined>(
  undefined,
);
