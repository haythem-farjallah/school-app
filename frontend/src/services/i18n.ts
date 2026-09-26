// src/services/i18n.ts
import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import  resourcesToBackend  from "i18next-resources-to-backend";


/* -------------------------------------------------------------------------- */
/*  Supported locales – only languages with a bundle in src/locales/<lng>/     */
/* -------------------------------------------------------------------------- */
export const supportedLocales = ["en", "fr", "zh-TW", "ar"] as const;
export type SupportedLocale = (typeof supportedLocales)[number];

export function isSupportedLocale(value: string | null | undefined): value is SupportedLocale {
  return supportedLocales.includes(value as SupportedLocale);
}

/* -------------------------------------------------------------------------- */
/*  Lazy JSON loader (Vite)                                                   */
/* -------------------------------------------------------------------------- */
// Vite grabs every JSON file in /locales/** and gives us an import function.
const localeFiles = import.meta.glob("../locales/**/*.json");

/**
 * Returns a dynamic import promise for the requested language + namespace.
 * i18next-resources-to-backend will call this under the hood.
 */
const loadLocale = (lng: string, ns: string) =>
  localeFiles[`../locales/${lng}/${ns}.json`]();
/* -------------------------------------------------------------------------- */
/*  i18n setup                                                                */
/* -------------------------------------------------------------------------- */
i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .use(resourcesToBackend(loadLocale))
  .init({
    fallbackLng: "en",
    supportedLngs: supportedLocales,
    detection: {
      order: ["path","localStorage", "navigator", "htmlTag"],
      caches: ["localStorage"],
      lookupFromPathIndex: 0,
    },
    interpolation: { escapeValue: false },
    debug: import.meta.env.MODE === "development",
    react: { useSuspense: true },
  });

export default i18n;
