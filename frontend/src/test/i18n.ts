import i18next, { type i18n as I18n, type Resource } from "i18next";
import { initReactI18next } from "react-i18next";
import type { SupportedLocale } from "@/services/i18n";

// The real bundles, loaded the same way as src/services/i18n.ts but synchronously.
const bundles = import.meta.glob<Record<string, unknown>>("../locales/*/translation.json", {
  eager: true,
  import: "default",
});

export const localeBundles: Record<string, Record<string, unknown>> = Object.fromEntries(
  Object.entries(bundles).map(([path, bundle]) => [path.split("/")[2], bundle]),
);

/** An i18next instance with the application's translations, for rendering components in tests. */
export function createTestI18n(lng: SupportedLocale = "en"): I18n {
  const resources: Resource = Object.fromEntries(
    Object.entries(localeBundles).map(([locale, translation]) => [locale, { translation }]),
  );
  const instance = i18next.createInstance();
  void instance.use(initReactI18next).init({
    lng,
    fallbackLng: "en",
    resources,
    initAsync: false,
    interpolation: { escapeValue: false },
    react: { useSuspense: false },
  });
  return instance;
}

/** Every leaf key of a nested translation bundle, e.g. "shell.search.label". */
export function translationKeys(bundle: Record<string, unknown>, prefix = ""): string[] {
  return Object.entries(bundle).flatMap(([key, value]) =>
    value !== null && typeof value === "object"
      ? translationKeys(value as Record<string, unknown>, `${prefix}${key}.`)
      : [`${prefix}${key}`],
  );
}
